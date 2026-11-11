package com.sajib.smsexpensetracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sajib.smsexpensetracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes the stored transactions to the transaction list screen. Reads
 * only: every write goes through the ingestion and intake use cases.
 */
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    repository: TransactionRepository
) : ViewModel() {

    /**
     * Every stored transaction, newest first, kept up to date by Room. Empty
     * until the first read finishes. Collection stops five seconds after the
     * last collector leaves, so a rotation does not restart the query.
     */
    val transactions: StateFlow<List<com.sajib.smsexpensetracker.data.db.Transaction>> =
        repository.getAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}
