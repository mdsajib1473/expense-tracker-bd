package com.sajib.smsexpensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sajib.smsexpensetracker.parser.core.TransactionType
import java.math.BigDecimal

/**
 * A single stored transaction, built from a parsed SMS.
 *
 * [receivedAt] is when the SMS itself arrived (epoch millis, from the SMS),
 * [insertedAt] is when this row was written to the database. They usually
 * match, but can differ during a historical import job, which matters later
 * for M8 sync conflict resolution.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val institutionName: String,
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String,
    val balance: BigDecimal?,
    val counterparty: String?,
    val reference: String?,
    val rawSms: String,
    val receivedAt: Long,
    val insertedAt: Long
)
