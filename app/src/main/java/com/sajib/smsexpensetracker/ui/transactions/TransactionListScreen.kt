package com.sajib.smsexpensetracker.ui.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sajib.smsexpensetracker.data.db.Transaction
import com.sajib.smsexpensetracker.parser.core.TransactionType

/**
 * Minimal, unstyled list proving data flows end to end: SMS in, parsed,
 * saved, and shown here. Real dashboard styling is M3's job, not this one.
 */
@Composable
fun TransactionListScreen(viewModel: TransactionListViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsState()

    if (transactions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet")
        }
        return
    }

    LazyColumn {
        items(transactions, key = { it.id }) { transaction ->
            TransactionRow(transaction)
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val isCredit = transaction.type == TransactionType.CREDIT
    val amountColor = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
    val sign = if (isCredit) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(transaction.institutionName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = transaction.counterparty ?: "-",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "$sign Tk ${transaction.amount}",
            color = amountColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
