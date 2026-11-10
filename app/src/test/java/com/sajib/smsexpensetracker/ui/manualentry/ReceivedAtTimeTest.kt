package com.sajib.smsexpensetracker.ui.manualentry

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Covers converting between the transaction time and the date and time pickers. */
class ReceivedAtTimeTest {

    private val dhaka = ZoneId.of("Asia/Dhaka")

    private fun millis(dateTime: LocalDateTime, zone: ZoneId = dhaka) = dateTime.atZone(zone).toInstant().toEpochMilli()
    private fun utcMidnight(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `seconds and millis are cut to the minute`() {
        val at = millis(LocalDateTime.of(2026, 3, 1, 10, 30, 45, 500_000_000))

        assertEquals(millis(LocalDateTime.of(2026, 3, 1, 10, 30)), ReceivedAtTime.toMinute(at))
    }

    @Test
    fun `picker date is the local date even when the UTC date differs`() {
        val earlyMorningInDhaka = millis(LocalDateTime.of(2026, 3, 1, 2, 0))

        assertEquals(utcMidnight(LocalDate.of(2026, 3, 1)), ReceivedAtTime.pickerDateMillis(earlyMorningInDhaka, dhaka))
    }

    @Test
    fun `picking a date keeps the local time of day`() {
        val at = millis(LocalDateTime.of(2026, 3, 1, 2, 15))

        val moved = ReceivedAtTime.withPickedDate(at, utcMidnight(LocalDate.of(2025, 12, 31)), dhaka)

        assertEquals(millis(LocalDateTime.of(2025, 12, 31, 2, 15)), moved)
    }

    @Test
    fun `picking a time keeps the local date and clears seconds`() {
        val at = millis(LocalDateTime.of(2026, 3, 1, 23, 59, 30))

        val moved = ReceivedAtTime.withPickedTime(at, 0, 5, dhaka)

        assertEquals(millis(LocalDateTime.of(2026, 3, 1, 0, 5)), moved)
        assertEquals(LocalTime.of(0, 5), ReceivedAtTime.localTime(moved, dhaka))
    }
}
