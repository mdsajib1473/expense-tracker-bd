package com.sajib.smsexpensetracker.data.db

import androidx.room.TypeConverter
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Room cannot store BigDecimal or enum types natively. BigDecimal is stored
 * as its exact plain string form, never as a Double, to preserve the
 * no-floating-point-for-money guarantee already locked in for this project.
 */
class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
