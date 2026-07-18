package com.oneledger.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT COUNT(*) FROM ledger_books")
    suspend fun countBooks(): Int

    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM categories WHERE transactionType = :type AND deletedAt IS NULL ORDER BY sortOrder, name")
    fun observeCategories(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM budgets ORDER BY periodStart DESC")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM savings_plans WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeSavingsPlans(): Flow<List<SavingsPlanEntity>>

    @Query(
        """
        SELECT
            t.id AS id,
            t.type AS type,
            t.amountMinor AS amountMinor,
            t.categoryId AS categoryId,
            COALESCE(c.name, CASE WHEN t.type = 'TRANSFER' THEN '转账' ELSE '未分类' END) AS categoryName,
            COALESCE(c.iconKey, 'swap') AS iconKey,
            COALESCE(c.colorHex, 4290756351) AS colorHex,
            t.accountId AS accountId,
            COALESCE(a.name, '未知账户') AS accountName,
            t.toAccountId AS toAccountId,
            t.note AS note,
            t.merchant AS merchant,
            t.occurredAt AS occurredAt
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        LEFT JOIN accounts a ON a.id = t.accountId
        WHERE t.deletedAt IS NULL
        ORDER BY t.occurredAt DESC, t.createdAt DESC
        """,
    )
    fun observeTransactions(): Flow<List<TransactionListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: LedgerBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsPlan(plan: SavingsPlanEntity)

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, deletedAt: Long)

    @Transaction
    suspend fun seed(
        book: LedgerBookEntity,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        budget: BudgetEntity,
        savingsPlan: SavingsPlanEntity,
    ) {
        insertBook(book)
        insertAccounts(accounts)
        insertCategories(categories)
        insertBudget(budget)
        insertSavingsPlan(savingsPlan)
    }
}
