package com.sajib.smsexpensetracker.parser.core

/**
 * The kind of event a transaction SMS represents.
 *
 * DEBIT and CREDIT cover money movement (wallets and banks).
 * BALANCE_INQUIRY covers SMS that only report a balance, with no money movement.
 * RECHARGE and AIRTIME cover telecom top-ups, kept separate from bank/wallet
 * transaction types so the dashboard can show them in their own section.
 */
enum class TransactionType {
    DEBIT,
    CREDIT,
    BALANCE_INQUIRY,
    RECHARGE,
    AIRTIME
}
