package com.sajib.smsexpensetracker.receiver

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.sajib.smsexpensetracker.parser.ParserTestMessages
import com.sajib.smsexpensetracker.parser.institutions.CraftedAmountParserTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Full flavor only. Delivers real SMS_RECEIVED broadcasts, built as PDUs and
 * decoded by the platform, to the real receiver with the real Hilt graph and
 * database. A crafted message must not crash the receiver's background
 * work, must be reported as "No match" and must not be stored or logged.
 * Every phone number, TrxID and amount is invented.
 */
@RunWith(RobolectricTestRunner::class)
class SmsReceiverCraftedMessageTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val uncaught = CopyOnWriteArrayList<Throwable>()
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    private val validBkash = ParserTestMessages.transactions.first { it.institutionName == "bKash" }
    private val marker = "CRAFTEDMARKER"
    private val craftedBkash =
        "Send Money Tk , to 01700000004 successful. Ref $marker. Fee Tk 5.00. Balance Tk 4,495.00. TrxID AAAAAAAAAA at 01/03/2026 10:00"

    @Before
    fun setUp() {
        ShadowLog.clear()
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> uncaught += throwable }
        shadowOf(app).grantPermissions(BROADCAST_SMS)
        val registered = shadowOf(app).registeredReceivers.any { it.broadcastReceiver is SmsReceiver }
        if (!registered) {
            ContextCompat.registerReceiver(
                app,
                SmsReceiver(),
                IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION),
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }

    private fun receiverLogs(): List<String> = ShadowLog.getLogsForTag(RECEIVER_TAG).map { it.msg.orEmpty() }

    /**
     * Sends [intent] and waits until the receiver's background work logs its
     * outcome, which it does last. Fails if that work threw instead.
     */
    private fun deliver(intent: Intent): String {
        val logsBefore = receiverLogs().size
        app.sendBroadcast(intent)
        repeat(WAIT_ATTEMPTS) {
            shadowOf(Looper.getMainLooper()).idle()
            uncaught.firstOrNull()?.let { throw AssertionError("receiver crashed with ${it.javaClass.name}") }
            receiverLogs().drop(logsBefore).firstOrNull()?.let { return it }
            Thread.sleep(WAIT_STEP_MILLIS)
        }
        throw AssertionError("receiver reported no outcome")
    }

    private fun deliver(sender: String, body: String): String = deliver(SmsPdu.smsReceivedIntent(sender, body))

    private fun storedBodies(): List<String> {
        val file = app.getDatabasePath(DATABASE_NAME)
        if (!file.exists()) return emptyList()
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT rawSms FROM transactions ORDER BY id", null).use { cursor ->
                generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
            }
        }
    }

    @Test
    fun `valid message delivered as PDUs is decoded, matched and stored`() {
        assertEquals("Matched: bKash", deliver(validBkash.sender, validBkash.body))

        assertEquals(listOf(validBkash.body), storedBodies())
    }

    @Test
    fun `crafted message does not crash the receiver and is neither stored nor logged`() {
        deliver(validBkash.sender, validBkash.body)

        val outcome = deliver("bKash", craftedBkash)

        assertEquals("No match", outcome)
        assertEquals(listOf(validBkash.body), storedBodies())
        assertFalse(ShadowLog.getLogs().any { it.msg.orEmpty().contains(marker) })
    }

    @Test
    fun `crafted message for every institution from its real sender is no match`() {
        val parsers = ParserTestMessages.allParsers()
        val firstCasePerInstitution = CraftedAmountParserTest.cases().distinctBy { it[0] }

        firstCasePerInstitution.forEach { (institutionName, case, body) ->
            val sender = parsers.single { it.institutionName == institutionName }.senderPatterns.first()

            assertEquals("$institutionName: $case", "No match", deliver(sender, body))
        }
        assertEquals(parsers.size, firstCasePerInstitution.size)
        assertEquals(emptyList<String>(), storedBodies())
    }

    @Test
    fun `message with no text body does not crash the receiver`() {
        val intent = SmsPdu.smsReceivedIntent(listOf(SmsPdu.nonTextPart("bKash", byteArrayOf(0x01, 0x02, 0x03))))
        val decoded = Telephony.Sms.Intents.getMessagesFromIntent(intent).single()
        assertEquals("bKash", decoded.originatingAddress)
        assertNull("the platform must report no text body for this message", decoded.messageBody)

        val outcome = deliver(intent)

        assertEquals("No match", outcome)
        assertEquals(emptyList<String>(), storedBodies())
    }

    @Test
    fun `text parts followed by a part with no text body are still joined and matched`() {
        val parts = SmsPdu.textParts(validBkash.sender, validBkash.body) +
            SmsPdu.nonTextPart(validBkash.sender, byteArrayOf(0x01))

        assertEquals("Matched: bKash", deliver(SmsPdu.smsReceivedIntent(parts)))
        assertEquals(listOf(validBkash.body), storedBodies())
    }

    private companion object {
        const val RECEIVER_TAG = "SmsReceiver"
        const val DATABASE_NAME = "sms_expense_tracker.db"
        const val BROADCAST_SMS = "android.permission.BROADCAST_SMS"
        const val WAIT_ATTEMPTS = 500
        const val WAIT_STEP_MILLIS = 10L
    }
}
