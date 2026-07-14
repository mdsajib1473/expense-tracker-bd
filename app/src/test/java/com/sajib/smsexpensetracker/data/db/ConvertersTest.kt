package com.sajib.smsexpensetracker.data.db

import com.sajib.smsexpensetracker.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `BigDecimal round trips through its plain string form`() {
        val original = BigDecimal("4479.65")
        val stored = converters.fromBigDecimal(original)
        val restored = converters.toBigDecimal(stored)

        assertEquals(original, restored)
    }

    @Test
    fun `null BigDecimal round trips as null`() {
        assertNull(converters.fromBigDecimal(null))
        assertNull(converters.toBigDecimal(null))
    }

    @Test
    fun `TransactionType round trips through its name`() {
        TransactionType.entries.forEach { type ->
            val stored = converters.fromTransactionType(type)
            val restored = converters.toTransactionType(stored)
            assertEquals(type, restored)
        }
    }
}
