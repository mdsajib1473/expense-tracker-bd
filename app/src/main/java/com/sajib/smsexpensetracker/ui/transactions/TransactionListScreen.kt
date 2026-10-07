package com.sajib.smsexpensetracker.ui.transactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sajib.smsexpensetracker.R
import com.sajib.smsexpensetracker.data.db.Transaction
import com.sajib.smsexpensetracker.parser.core.TransactionType
import com.sajib.smsexpensetracker.ui.backupimport.BackupImportDialog
import com.sajib.smsexpensetracker.ui.backupimport.BackupImportViewModel

/**
 * Some file managers label an SMS Backup & Restore export oddly, so the
 * picker also offers every file type after the two XML ones.
 */
private val BACKUP_MIME_TYPES = arrayOf("text/xml", "application/xml", "*/*")

/**
 * Minimal, unstyled list proving data flows end to end: SMS in, parsed,
 * saved, and shown here. Real dashboard styling is M3's job, not this one.
 * Importing from an SMS Backup & Restore file is reachable from the app bar
 * and, while the list is empty, from the empty state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    importViewModel: BackupImportViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val importState by importViewModel.state.collectAsState()

    val pickBackupFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importViewModel.import(uri)
    }
    val onImportClick = { pickBackupFile.launch(BACKUP_MIME_TYPES) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onImportClick) {
                        Text(stringResource(R.string.backup_import_action))
                    }
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            EmptyState(onImportClick = onImportClick, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(contentPadding = padding) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRow(transaction)
                }
            }
        }
    }

    BackupImportDialog(
        state = importState,
        onCancel = importViewModel::cancel,
        onDismiss = importViewModel::dismiss
    )
}

@Composable
private fun EmptyState(onImportClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.transactions_empty), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.backup_import_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onImportClick) {
            Text(stringResource(R.string.backup_import_empty_button))
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val isCredit = transaction.type == TransactionType.CREDIT
    val amountColor = colorResource(if (isCredit) R.color.amount_credit else R.color.amount_debit)
    val sign = if (isCredit) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(transaction.institutionName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = transaction.counterparty ?: stringResource(R.string.transactions_no_counterparty),
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
