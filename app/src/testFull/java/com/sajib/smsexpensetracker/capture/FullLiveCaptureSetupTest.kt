package com.sajib.smsexpensetracker.capture

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.sajib.smsexpensetracker.importer.FirstLaunchTracker
import com.sajib.smsexpensetracker.importer.InboxImporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Full flavor only. Covers live capture with the SMS permission optional:
 * nothing starts and nothing fails while it is declined or never answered,
 * and the one-time inbox import runs once as soon as it is granted.
 */
@RunWith(RobolectricTestRunner::class)
class FullLiveCaptureSetupTest {

    private class CountingImporter : InboxImporter {
        var runs = 0
        override suspend fun importInbox() {
            runs++
        }
    }

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val importer = CountingImporter()
    private val tracker = FirstLaunchTracker(app)
    private val setup = FullLiveCaptureSetup(app, importer, tracker)

    private fun grant(vararg permissions: String) = shadowOf(app).grantPermissions(*permissions)
    private fun deny(vararg permissions: String) = shadowOf(app).denyPermissions(*permissions)

    @Test
    fun `never answered permission starts nothing`() = runTest {
        setup.startCapture()

        assertEquals(0, importer.runs)
        assertFalse(tracker.hasImportedHistory())
    }

    @Test
    fun `declined permission starts nothing on any resume`() = runTest {
        deny(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

        repeat(3) { setup.startCapture() }

        assertEquals(0, importer.runs)
        assertFalse(tracker.hasImportedHistory())
    }

    @Test
    fun `only one of the two permissions starts nothing`() = runTest {
        grant(Manifest.permission.RECEIVE_SMS)
        deny(Manifest.permission.READ_SMS)

        setup.startCapture()

        assertEquals(0, importer.runs)
    }

    @Test
    fun `permission granted later runs the inbox import exactly once`() = runTest {
        setup.startCapture()
        grant(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

        setup.startCapture()
        setup.startCapture()

        assertEquals(1, importer.runs)
        assertTrue(tracker.hasImportedHistory())
    }

    @Test
    fun `history imported on an earlier launch is not read again`() = runTest {
        grant(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        tracker.markHistoryImported()

        FullLiveCaptureSetup(app, importer, tracker).startCapture()

        assertEquals(0, importer.runs)
    }

    @Test
    fun `full flavor offers the optional onboarding step`() {
        assertTrue(setup.hasOnboardingStep)
    }

    @Test
    fun `the required permissions are the two SMS permissions`() {
        assertEquals(
            setOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
            SmsPermissions.REQUIRED.toSet()
        )
    }
}
