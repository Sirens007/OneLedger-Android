package com.oneledger.app.data.repository

import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.data.local.BudgetEntity
import com.oneledger.app.data.local.CategoryEntity
import com.oneledger.app.data.local.SavingsPlanEntity
import com.oneledger.app.data.local.TransactionListItem
import com.oneledger.app.domain.model.NewTransaction
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    val accounts: Flow<List<AccountEntity>>
    val transactions: Flow<List<TransactionListItem>>
    val budgets: Flow<List<BudgetEntity>>
    val savingsPlans: Flow<List<SavingsPlanEntity>>

    fun categories(type: String): Flow<List<CategoryEntity>>

    suspend fun ensureSeedData()
    suspend fun addTransaction(transaction: NewTransaction): String
    suspend fun updateTransaction(id: String, transaction: NewTransaction)
    suspend fun deleteTransaction(id: String)
    suspend fun restoreTransaction(id: String)
    suspend fun undoTransaction(id: String)
    suspend fun saveBudget(
        periodStart: Long,
        periodEnd: Long,
        limitMinor: Long,
        categoryId: String? = null,
    )
}
