package com.sajib.smsexpensetracker.capture

import android.Manifest
import android.app.Activity
import android.app.Application
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Full flavor only. Covers the decision behind the onboarding step's button:
 * request the SMS permission while Android still shows its prompt, open app
 * settings once it no longer does, and nothing to ask once it is granted.
 */
@RunWith(RobolectricTestRunner::class)
class SmsAccessPolicyTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val store = SmsPermissionRequestStore(app)

    private fun grant(vararg permissions: String) = shadowOf(app).grantPermissions(*permissions)

    private fun rationale(permission: String, show: Boolean) =
        shadowOf(app.packageManager).setShouldShowRequestPermissionRationale(permission, show)

    private fun currentState() = SmsAccessPolicy.currentState(app, activity, store)

    @Test
    fun `granted wins whatever the history says`() {
        listOf(false, true).forEach { requestedBefore ->
            listOf(false, true).forEach { shouldShowRationale ->
                assertEquals(
                    SmsAccessState.GRANTED,
                    SmsAccessPolicy.stateFor(granted = true, requestedBefore, shouldShowRationale)
                )
            }
        }
    }

    @Test
    fun `never requested can be requested, whatever Android says about a rationale`() {
        listOf(false, true).forEach { shouldShowRationale ->
            assertEquals(
                SmsAccessState.CAN_REQUEST,
                SmsAccessPolicy.stateFor(granted = false, requestedBefore = false, shouldShowRationale)
            )
        }
    }

    @Test
    fun `denied once can be requested again because Android still asks`() {
        assertEquals(
            SmsAccessState.CAN_REQUEST,
            SmsAccessPolicy.stateFor(granted = false, requestedBefore = true, shouldShowRationale = true)
        )
    }

    @Test
    fun `requested before with no rationale left is permanently denied`() {
        assertEquals(
            SmsAccessState.PERMANENTLY_DENIED,
            SmsAccessPolicy.stateFor(granted = false, requestedBefore = true, shouldShowRationale = false)
        )
    }

    @Test
    fun `app settings intent opens the details screen of the given package and carries nothing else`() {
        val intent = SmsAccessPolicy.appSettingsIntent("com.sample.app")

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals(Uri.parse("package:com.sample.app"), intent.data)
        assertNull(intent.extras)
    }

    @Test
    fun `request history starts empty and is kept once marked`() {
        assertFalse(store.wasRequested())

        store.markRequested()

        assertTrue(store.wasRequested())
        assertTrue(SmsPermissionRequestStore(app).wasRequested())
    }

    @Test
    fun `rationale is wanted when Android wants it for either permission`() {
        assertFalse(SmsPermissions.shouldShowRationale(activity))

        rationale(Manifest.permission.READ_SMS, show = true)

        assertTrue(SmsPermissions.shouldShowRationale(activity))
    }

    @Test
    fun `fresh install can request`() {
        assertEquals(SmsAccessState.CAN_REQUEST, currentState())
    }

    @Test
    fun `first denial still allows a request`() {
        store.markRequested()
        rationale(Manifest.permission.RECEIVE_SMS, show = true)
        rationale(Manifest.permission.READ_SMS, show = true)

        assertEquals(SmsAccessState.CAN_REQUEST, currentState())
    }

    @Test
    fun `second denial switches to app settings`() {
        store.markRequested()
        rationale(Manifest.permission.RECEIVE_SMS, show = false)
        rationale(Manifest.permission.READ_SMS, show = false)

        assertEquals(SmsAccessState.PERMANENTLY_DENIED, currentState())
    }

    @Test
    fun `granting in app settings afterwards is seen as granted`() {
        store.markRequested()
        assertEquals(SmsAccessState.PERMANENTLY_DENIED, currentState())

        grant(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

        assertEquals(SmsAccessState.GRANTED, currentState())
    }

    @Test
    fun `only one of the two permissions granted is not granted`() {
        store.markRequested()
        grant(Manifest.permission.RECEIVE_SMS)

        assertEquals(SmsAccessState.PERMANENTLY_DENIED, currentState())
    }

    @Test
    fun `without an activity the rationale counts as not wanted`() {
        rationale(Manifest.permission.READ_SMS, show = true)

        assertEquals(SmsAccessState.CAN_REQUEST, SmsAccessPolicy.currentState(app, activity = null, store))
        store.markRequested()
        assertEquals(SmsAccessState.PERMANENTLY_DENIED, SmsAccessPolicy.currentState(app, activity = null, store))
    }
}
