package com.sajib.smsexpensetracker.ui.manualentry

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Conversions between the stored transaction time (epoch millis) and the
 * Material date and time pickers. The date picker works in UTC midnights,
 * while the transaction time is a moment in the user's [ZoneId].
 */
internal object ReceivedAtTime {

    /** [epochMillis] cut down to whole minutes, the precision the pickers can show and set. */
    fun toMinute(epochMillis: Long): Long =
        Instant.ofEpochMilli(epochMillis).truncatedTo(ChronoUnit.MINUTES).toEpochMilli()

    /** The local calendar date of [epochMillis] in [zone], as the UTC midnight the date picker expects. */
    fun pickerDateMillis(epochMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** [epochMillis] moved to the date picked as [pickedUtcMillis], keeping its local time of day in [zone]. */
    fun withPickedDate(epochMillis: Long, pickedUtcMillis: Long, zone: ZoneId): Long {
        val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return Instant.ofEpochMilli(epochMillis).atZone(zone).with(pickedDate).toInstant().toEpochMilli()
    }

    /** [epochMillis] moved to [hour]:[minute] on the same local date in [zone], with zero seconds. */
    fun withPickedTime(epochMillis: Long, hour: Int, minute: Int, zone: ZoneId): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone)
            .with(LocalTime.of(hour, minute))
            .toInstant().toEpochMilli()

    /** The local time of day of [epochMillis] in [zone]. */
    fun localTime(epochMillis: Long, zone: ZoneId): LocalTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
}
