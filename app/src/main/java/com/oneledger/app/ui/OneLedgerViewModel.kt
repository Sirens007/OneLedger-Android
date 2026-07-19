package com.oneledger.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.data.local.AccountBalanceItem
import com.oneledger.app.data.local.BudgetEntity
import com.oneledger.app.data.local.CategoryEntity
import com.oneledger.app.data.local.SavingsPlanEntity
import com.oneledger.app.data.local.TransactionListItem
import com.oneledger.app.data.repository.LedgerRepository
import com.oneledger.app.domain.model.NewTransaction
import com.oneledger.app.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OneLedgerUiState(
    val activeBookId: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val accountBalances: List<AccountBalanceItem> = emptyList(),
    val transactions: List<TransactionListItem> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val savingsPlans: List<SavingsPlanEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
)

private data class LedgerData(
    val accounts: List<AccountEntity>,
    val accountBalances: List<AccountBalanceItem>,
    val transactions: List<TransactionListItem>,
    val budgets: List<BudgetEntity>,
)

private data class PlanningData(
    val plans: List<SavingsPlanEntity>,
    val expenseCategories: List<CategoryEntity>,
    val incomeCategories: List<CategoryEntity>,
)

class OneLedgerViewModel(
    private val repository: LedgerRepository,
) : ViewModel() {
    private val ledgerData = combine(
        repository.accounts,
        repository.accountBalances,
        repository.transactions,
        repository.budgets,
    ) { accounts, accountBalances, transactions, budgets ->
        LedgerData(accounts, accountBalances, transactions, budgets)
    }

    private val planningData = combine(
        repository.savingsPlans,
        repository.categories(TransactionType.EXPENSE),
        repository.categories(TransactionType.INCOME),
    ) { plans, expenseCategories, incomeCategories ->
        PlanningData(plans, expenseCategories, incomeCategories)
    }

    val uiState = combine(ledgerData, planningData, repository.activeBookId) { ledger, planning, activeBookId ->
        OneLedgerUiState(
            activeBookId = activeBookId,
            accounts = ledger.accounts,
            accountBalances = ledger.accountBalances,
            transactions = ledger.transactions,
            budgets = ledger.budgets,
            savingsPlans = planning.plans,
            expenseCategories = planning.expenseCategories,
            incomeCategories = planning.incomeCategories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OneLedgerUiState(),
    )

    init {
        viewModelScope.launch { repository.ensureSeedData() }
    }

    fun addTransaction(transaction: NewTransaction, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            onSaved(repository.addTransaction(transaction))
        }
    }

    fun selectBook(bookId: String, onComplete: (Result<Unit>) -> Unit = {}) = runMutation(onComplete) {
        repository.selectBook(bookId)
    }

    fun undoTransaction(id: String) {
        viewModelScope.launch { repository.undoTransaction(id) }
    }

    fun updateTransaction(
        id: String,
        transaction: NewTransaction,
        onComplete: (Result<Unit>) -> Unit,
    ) = runMutation(onComplete) {
        repository.updateTransaction(id, transaction)
    }

    fun deleteTransaction(id: String, onComplete: (Result<Unit>) -> Unit) = runMutation(onComplete) {
        repository.deleteTransaction(id)
    }

    fun restoreTransaction(id: String, onComplete: (Result<Unit>) -> Unit = {}) = runMutation(onComplete) {
        repository.restoreTransaction(id)
    }

    fun saveBudget(
        periodStart: Long,
        periodEnd: Long,
        limitMinor: Long,
        categoryId: String? = null,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            repository.saveBudget(
                periodStart = periodStart,
                periodEnd = periodEnd,
                limitMinor = limitMinor,
                categoryId = categoryId,
            )
            onSaved()
        }
    }

    private fun runMutation(
        onComplete: (Result<Unit>) -> Unit,
        mutation: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                mutation()
                onComplete(Result.success(Unit))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                onComplete(Result.failure(error))
            }
        }
    }
}

class OneLedgerViewModelFactory(
    private val repository: LedgerRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OneLedgerViewModel::class.java))
        return OneLedgerViewModel(repository) as T
    }
}
