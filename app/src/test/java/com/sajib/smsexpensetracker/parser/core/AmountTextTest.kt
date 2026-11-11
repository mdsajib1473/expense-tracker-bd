package com.sajib.smsexpensetracker.parser.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Covers the one conversion every parser uses for amount and balance text:
 * numbers come out exactly as BigDecimal reads them, and text that is not a
 * number gives null instead of an exception.
 */
class AmountTextTest {

    @Test
    fun `plain and comma grouped numbers keep their exact value and scale`() {
        mapOf(
            "500.00" to "500.00",
            "4,495.00" to "4495.00",
            "1,00,000.50" to "100000.50",
            "47800" to "47800",
            "30" to "30",
            "0.00" to "0.00",
            "5032.9" to "5032.9"
        ).forEach { (text, expected) ->
            assertEquals(text, BigDecimal(expected), parseAmountOrNull(text))
        }
    }

    @Test
    fun `a leading minus is kept`() {
        assertEquals(BigDecimal("-1452200"), parseAmountOrNull("-1452200"))
        assertEquals(BigDecimal("-200258.75"), parseAmountOrNull("-200,258.75"))
    }

    @Test
    fun `text with no digits is not a number`() {
        listOf("", ",", ",,,,", ".", ",.", "-", "-,", "-.", " ").forEach { text ->
            assertNull("[$text]", parseAmountOrNull(text))
        }
    }

    @Test
    fun `text that is not one number is not a number`() {
        listOf("1.2.3", "5..00", "12a", "Tk 500", "5 00", "1e", "--5", "5-").forEach { text ->
            assertNull("[$text]", parseAmountOrNull(text))
        }
    }

    @Test
    fun `every number BigDecimal accepts gives the same result as its constructor`() {
        listOf("5.", ".5", "-.5", "007", "1E+3", "0", "123456789012345678901234567890.12").forEach { text ->
            assertEquals(text, BigDecimal(text), parseAmountOrNull(text))
        }
    }
}
