package com.sajib.smsexpensetracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sajib.smsexpensetracker.parser.core.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
            amount = BigDecimal("5075.00"),
            currency = "BDT",
            balance = BigDecimal("6075.50"),
            counterparty = "01735814791",
            reference = "BLU1H6X87P",
            rawSms = "You have received Tk 5,075.00 from 01735814791. Fee Tk 0.00. Balance Tk 6,075.50. TrxID BLU1H6X87P at 30/12/2024 13:22",
            receivedAt = 1735562520000L,
            insertedAt = 1735562520000L
        )

        dao.insert(transaction)
        val stored = dao.getAll().first()

        assertEquals(1, stored.size)
        assertEquals(BigDecimal("5075.00"), stored[0].amount)
        assertEquals(BigDecimal("6075.50"), stored[0].balance)
        assertEquals(TransactionType.CREDIT, stored[0].type)
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
