package com.sajib.smsexpensetracker.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sajib.smsexpensetracker.data.db.AppDatabase
import com.sajib.smsexpensetracker.data.repository.TransactionRepository
import com.sajib.smsexpensetracker.domain.model.ManualEntryPreparation
import com.sajib.smsexpensetracker.domain.model.ManualEntrySaveResult
import com.sajib.smsexpensetracker.parser.ParserTestMessages
import com.sajib.smsexpensetracker.parser.core.ParserEngine
import com.sajib.smsexpensetracker.parser.core.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

/**
 * Runs the share and paste intake against a real in-memory Room database
 * through the real repository, so the duplicate rule and the similar
 * transaction query are the production ones. Every value is invented.
 */
@RunWith(RobolectricTestRunner::class)
class ManualEntryUseCaseRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var prepare: PrepareManualEntryUseCase
    private lateinit var save: SaveManualEntryUseCase
    private lateinit var ingest: IngestSmsUseCase

    private val uttaraNoBalance =
        "Dear Customer, BDT 5000 has been credited to your A/C No 032300000000003 from Sample Road Branch"
    private val nagadPayment =
        "Payment to 'Sample Ministry' is Successful.\nAmount: Tk  107.07\nTxnID: 72DDDDDD\nBalance: Tk 32.90\n03/11/2023 13:58"
    private val otp = "975310"
    private val otpBody = "Dear Customer, $otp is your OTP for e-com transaction of Debit Card no# 0000****0000."

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TransactionRepository(db.transactionDao())
        val engine = ParserEngine(ParserTestMessages.allParsers())
        prepare = PrepareManualEntryUseCase(engine, repository)
        save = SaveManualEntryUseCase(repository)
        ingest = IngestSmsUseCase(engine, repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `preview then save stores one row with the confirmed time and no message text`() = runTest {
        val preview = prepare(nagadPayment) as ManualEntryPreparation.Preview

        assertEquals(ManualEntrySaveResult.Saved, save(preview, receivedAt = 1_700_000_000_000L))

        val stored = repository.getAll().first().single()
        assertEquals("Nagad", stored.institutionName)
        assertEquals(TransactionType.DEBIT, stored.type)
        assertEquals(BigDecimal("107.07"), stored.amount)
        assertEquals(BigDecimal("32.90"), stored.balance)
        assertEquals("72DDDDDD", stored.reference)
        assertNull(stored.counterparty)
        assertEquals("", stored.rawSms)
        assertEquals(1_700_000_000_000L, stored.receivedAt)
    }

    @Test
    fun `saving again at the same time is a duplicate and stores nothing new`() = runTest {
        val preview = prepare(nagadPayment) as ManualEntryPreparation.Preview
        save(preview, receivedAt = 5_000L)

        assertEquals(ManualEntrySaveResult.Duplicate, save(preview, receivedAt = 5_000L))
        assertEquals(1, repository.getAll().first().size)
    }

    @Test
    fun `similar warning appears once a row with the same amount and balance exists at any time`() = runTest {
        assertFalse((prepare(nagadPayment) as ManualEntryPreparation.Preview).similarExists)

        ingest(sender = "NAGAD", body = nagadPayment, receivedAt = 9_000L)

        val again = prepare(nagadPayment) as ManualEntryPreparation.Preview
        assertTrue(again.similarExists)
        assertEquals(ManualEntrySaveResult.Saved, save(again, receivedAt = 10_000L))
        assertEquals(2, repository.getAll().first().size)
    }

    @Test
    fun `similar check matches a null balance only to a stored null balance`() = runTest {
        val preview = prepare(uttaraNoBalance) as ManualEntryPreparation.Preview
        assertNull(preview.balance)
        save(preview, receivedAt = 1_000L)

        assertTrue((prepare(uttaraNoBalance) as ManualEntryPreparation.Preview).similarExists)
        assertFalse(
            repository.hasSimilar("Uttara Bank", TransactionType.CREDIT, BigDecimal("5000"), BigDecimal("1.00"))
        )
    }

    @Test
    fun `no match, too long and blank texts persist nothing and never reach the database`() = runTest {
        assertEquals(ManualEntryPreparation.NoMatch, prepare(otpBody))
        assertEquals(ManualEntryPreparation.TooLong, prepare(nagadPayment + " ".repeat(PrepareManualEntryUseCase.MAX_TEXT_LENGTH)))
        assertEquals(ManualEntryPreparation.Blank, prepare(" "))

        val stored = repository.getAll().first()
        assertTrue(stored.isEmpty())
        assertFalse(stored.any { it.rawSms.contains(otp) })
    }
}
