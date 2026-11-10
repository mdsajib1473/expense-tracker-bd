package com.sajib.smsexpensetracker.ui.manualentry

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.style.StyleSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Covers reading only EXTRA_TEXT from a text/plain share. Every value is invented. */
@RunWith(RobolectricTestRunner::class)
class SharedTextIntentTest {

    private val body = "Payment Tk 100.00 to SAMPLE MERCHANT-RM00001 is successful. Balance Tk 600.00. TrxID HHHHHHHHHH at 01/07/2026 10:00"
    private val streamUri = Uri.parse("content://sample.provider/secret-file.txt")

    private fun share(type: String? = "text/plain", action: String = Intent.ACTION_SEND) =
        Intent(action).setType(type)

    @Test
    fun `text plain share gives its EXTRA_TEXT`() {
        assertEquals(body, SharedTextIntent.extractText(share().putExtra(Intent.EXTRA_TEXT, body)))
    }

    @Test
    fun `mime type with parameters or upper case is still text plain`() {
        assertEquals(body, SharedTextIntent.extractText(share("TEXT/PLAIN; charset=utf-8").putExtra(Intent.EXTRA_TEXT, body)))
    }

    @Test
    fun `styled EXTRA_TEXT is read as a plain string`() {
        val styled = SpannableString(body).apply { setSpan(StyleSpan(0), 0, 7, 0) }

        val text = SharedTextIntent.extractText(share().putExtra(Intent.EXTRA_TEXT, styled))

        assertEquals(body, text)
        assertEquals(String::class.java, text!!::class.java)
    }

    @Test
    fun `every other extra, the clip data and the data uri are ignored`() {
        val intent = share()
            .putExtra(Intent.EXTRA_TEXT, body)
            .putExtra(Intent.EXTRA_SUBJECT, "Sample subject")
            .putExtra(Intent.EXTRA_HTML_TEXT, "<b>Sample html</b>")
            .putExtra(Intent.EXTRA_STREAM, streamUri)
        intent.clipData = ClipData.newRawUri("sample", streamUri)
        intent.setDataAndType(streamUri, "text/plain")

        assertEquals(body, SharedTextIntent.extractText(intent))
    }

    @Test
    fun `share with only a stream gives empty text and never the uri`() {
        val intent = share().putExtra(Intent.EXTRA_STREAM, streamUri)
        intent.clipData = ClipData.newRawUri("sample", streamUri)

        val text = SharedTextIntent.extractText(intent)

        assertEquals("", text)
        assertFalse(text.orEmpty().contains("secret-file"))
    }

    @Test
    fun `other actions and types are not a share`() {
        assertNull(SharedTextIntent.extractText(share(type = "image/png").putExtra(Intent.EXTRA_TEXT, body)))
        assertNull(SharedTextIntent.extractText(share(type = null).putExtra(Intent.EXTRA_TEXT, body)))
        assertNull(SharedTextIntent.extractText(share(action = Intent.ACTION_VIEW).putExtra(Intent.EXTRA_TEXT, body)))
        assertNull(SharedTextIntent.extractText(share(action = Intent.ACTION_SEND_MULTIPLE).putExtra(Intent.EXTRA_TEXT, body)))
        assertNull(SharedTextIntent.extractText(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)))
    }

    @Test
    fun `launch intent is handled only on a fresh launch`() {
        val intent = share().putExtra(Intent.EXTRA_TEXT, body)

        assertEquals(true, SharedTextIntent.isFreshLaunch(intent, isRecreated = false))
        assertEquals(false, SharedTextIntent.isFreshLaunch(intent, isRecreated = true))
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        assertEquals(false, SharedTextIntent.isFreshLaunch(intent, isRecreated = false))
    }
}
