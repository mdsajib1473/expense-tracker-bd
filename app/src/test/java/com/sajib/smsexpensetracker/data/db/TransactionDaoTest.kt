package com.sajib.smsexpensetracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sajib.smsexpensetracker.parser.core.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
class TransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert then getAll returns the transaction with amount preserved exactly`() = runTest {
        val transaction = Transaction(
            institutionName = "bKash",
            type = TransactionType.CREDIT,
            amount = BigDecimal("5000.00"),
            currency = "BDT",
            balance = BigDecimal("6000.00"),
            counterparty = "01700000006",
            reference = "CCCCCCCCCC",
            rawSms = "You have received Tk 5,000.00 from 01700000006. Fee Tk 0.00. Balance Tk 6,000.00. TrxID CCCCCCCCCC at 01/12/2024 10:00",
            receivedAt = 1733047200000L,
            insertedAt = 1733047200000L
        )

        dao.insert(transaction)
        val stored = dao.getAll().first()

        assertEquals(1, stored.size)
        assertEquals(BigDecimal("5000.00"), stored[0].amount)
        assertEquals(BigDecimal("6000.00"), stored[0].balance)
        assertEquals(TransactionType.CREDIT, stored[0].type)
    }

    @Test
    fun `insertIfAbsent skips an identical row including a null balance`() = runTest {
        val row = Transaction(
            institutionName = "Uttara Bank", type = TransactionType.CREDIT, amount = BigDecimal("5000"),
            currency = "BDT", balance = null, counterparty = "Sample Road Branch", reference = null,
            rawSms = "test", receivedAt = 2000L, insertedAt = 2000L
        )

        assertTrue(dao.insertIfAbsent(row))
        assertFalse(dao.insertIfAbsent(row.copy(insertedAt = 3000L)))
        assertEquals(1, dao.getAll().first().size)
    }

    @Test
    fun `insertIfAbsent keeps rows that differ in balance or timestamp`() = runTest {
        val row = Transaction(
            institutionName = "bKash", type = TransactionType.DEBIT, amount = BigDecimal("100.00"),
            currency = "BDT", balance = BigDecimal("500.00"), counterparty = "X", reference = "A1",
            rawSms = "test", receivedAt = 1000L, insertedAt = 1000L
        )

        assertTrue(dao.insertIfAbsent(row))
        assertTrue(dao.insertIfAbsent(row.copy(balance = BigDecimal("400.00"))))
        assertTrue(dao.insertIfAbsent(row.copy(receivedAt = 1001L)))
        assertEquals(3, dao.getAll().first().size)
    }

    @Test
    fun `getByDateRange excludes transactions outside the range`() = runTest {
        val inRange = Transaction(
            institutionName = "bKash", type = TransactionType.DEBIT, amount = BigDecimal("100.00"),
            currency = "BDT", balance = BigDecimal("500.00"), counterparty = "X", reference = "A1",
            rawSms = "test", receivedAt = 1000L, insertedAt = 1000L
        )
        val outOfRange = inRange.copy(reference = "A2", receivedAt = 5000L, insertedAt = 5000L)

        dao.insert(inRange)
        dao.insert(outOfRange)

        val result = dao.getByDateRange(0L, 2000L).first()

        assertEquals(1, result.size)
        assertEquals("A1", result[0].reference)
    }
}
