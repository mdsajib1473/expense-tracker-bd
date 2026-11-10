package com.sajib.smsexpensetracker

import android.content.Intent
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import com.sajib.smsexpensetracker.ui.manualentry.ManualEntryState
import com.sajib.smsexpensetracker.ui.manualentry.ManualEntryViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/**
 * Covers the share target end to end in the real activity with the real
 * Hilt graph: a shared message is checked on cold start and while the
 * activity is open, and is never processed again after recreation or a
 * relaunch from Recents. Runs in both flavors; in the full flavor no SMS
 * permission is granted, so this also shows sharing is not gated on it.
 * Every value is invented.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityShareTest {

    private val body =
        "Payment Tk 100.00 to SAMPLE MERCHANT-RM00001 is successful. Balance Tk 600.00. TrxID HHHHHHHHHH at 01/07/2026 10:00"

    private fun shareIntent(): Intent =
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, body)

    private fun ActivityController<MainActivity>.manualEntry(): ManualEntryViewModel =
        ViewModelProvider(get())[ManualEntryViewModel::class.java]

    /** Runs the main looper until the check, which queries Room on a background thread, has finished. */
    private fun ManualEntryViewModel.settled(): ManualEntryState {
        repeat(SETTLE_ATTEMPTS) {
            shadowOf(Looper.getMainLooper()).idle()
            if (state.value != ManualEntryState.Checking) return state.value
            Thread.sleep(SETTLE_STEP_MILLIS)
        }
        return state.value
    }

    @Test
    fun `cold start share is checked and the consumed intent no longer holds the text`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, shareIntent()).setup()

        val state = controller.manualEntry().settled()

        assertTrue(state.toString(), state is ManualEntryState.Preview)
        assertNull(controller.get().intent.getStringExtra(Intent.EXTRA_TEXT))
        assertNotEquals(Intent.ACTION_SEND, controller.get().intent.action)
    }

    @Test
    fun `recreation after the share was handled does not process it again`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, shareIntent()).setup()
        controller.manualEntry().settled()
        controller.manualEntry().dismiss()

        controller.recreate()

        assertEquals(ManualEntryState.Idle, controller.manualEntry().settled())
    }

    @Test
    fun `relaunch from recents with the original share intent does not process it again`() {
        val replayed = shareIntent().addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)

        val controller = Robolectric.buildActivity(MainActivity::class.java, replayed).setup()

        assertEquals(ManualEntryState.Idle, controller.manualEntry().settled())
    }

    @Test
    fun `share while the app is already open arrives through onNewIntent`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertEquals(ManualEntryState.Idle, controller.manualEntry().settled())

        controller.newIntent(shareIntent())

        assertTrue(controller.manualEntry().settled() is ManualEntryState.Preview)
    }

    private companion object {
        const val SETTLE_ATTEMPTS = 200
        const val SETTLE_STEP_MILLIS = 10L
    }
}
