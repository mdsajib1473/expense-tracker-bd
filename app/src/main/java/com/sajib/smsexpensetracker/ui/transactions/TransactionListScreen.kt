package com.sajib.smsexpensetracker.ui.transactions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sajib.smsexpensetracker.R
import com.sajib.smsexpensetracker.data.db.Transaction
import com.sajib.smsexpensetracker.ui.backupimport.BackupImportDialog
import com.sajib.smsexpensetracker.ui.backupimport.BackupImportViewModel
import com.sajib.smsexpensetracker.ui.common.labelRes

/**
 * Some file managers label an SMS Backup & Restore export oddly, so the
 * picker also offers every file type after the two XML ones.
 */
private val BACKUP_MIME_TYPES = arrayOf("text/xml", "application/xml", "*/*")

/**
 * Minimal, unstyled list proving data flows end to end: SMS in, parsed,
 * saved, and shown here. Real dashboard styling is M3's job, not this one.
 * The app bar has "Add from a message" ([onAddFromMessage]) and a menu with
 * "Import from backup" and "How to add transactions" ([onShowHowToAdd]);
 * while the list is empty, the empty state offers import and add too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onAddFromMessage: () -> Unit,
    onShowHowToAdd: () -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel(),
    importViewModel: BackupImportViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val importState by importViewModel.state.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    val pickBackupFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importViewModel.import(uri)
    }
    val onImportClick = { pickBackupFile.launch(BACKUP_MIME_TYPES) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = onAddFromMessage) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.manual_entry_action)
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.menu_more_options)
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.backup_import_action)) },
                                onClick = {
                                    menuOpen = false
                                    onImportClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_how_to_add)) },
                                onClick = {
                                    menuOpen = false
                                    onShowHowToAdd()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (transactions.isEmpty()) {
            EmptyState(
                onImportClick = onImportClick,
                onAddFromMessage = onAddFromMessage,
                modifier = Modifier.padding(padding)
            )
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
private fun EmptyState(
    onImportClick: () -> Unit,
    onAddFromMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(R.dimen.spacing_xlarge)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large), Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.transactions_empty),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.transactions_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.backup_import_empty_button), textAlign = TextAlign.Center)
        }
        OutlinedButton(onClick = onAddFromMessage, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.manual_entry_action), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val kind = transaction.amountKind()
    val figure = transaction.displayedFigure().toPlainString()
    val amountText = when (kind) {
        AmountKind.MONEY_IN -> stringResource(R.string.transaction_amount_credit, figure)
        AmountKind.MONEY_OUT -> stringResource(R.string.transaction_amount_debit, figure)
        AmountKind.BALANCE -> stringResource(R.string.transaction_amount_balance, figure)
    }
    val amountColor = when (kind) {
        AmountKind.MONEY_IN -> colorResource(R.color.amount_credit)
        AmountKind.MONEY_OUT -> colorResource(R.color.amount_debit)
        AmountKind.BALANCE -> LocalContentColor.current
    }
    val detail = if (kind == AmountKind.BALANCE) {
        stringResource(transaction.type.labelRes())
    } else {
        transaction.counterparty ?: stringResource(R.string.transactions_no_counterparty)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.min_touch_target))
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_medium)
            )
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(transaction.institutionName, style = MaterialTheme.typography.bodyLarge)
            Text(text = detail, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = amountText,
            color = amountColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
