package com.oneledger.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT id FROM ledger_books WHERE isDefault = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getDefaultBookId(): String?

    @Query("SELECT id FROM ledger_books ORDER BY createdAt, id LIMIT 1")
    suspend fun getFirstBookId(): String?

    @Query("SELECT EXISTS(SELECT 1 FROM ledger_books WHERE id = :bookId)")
    suspend fun bookExists(bookId: String): Boolean

    @Query("SELECT * FROM accounts WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY sortOrder, name")
    fun observeAccounts(bookId: String): Flow<List<AccountEntity>>

    @Query(
        """
        SELECT
            a.id AS accountId,
            a.openingBalanceMinor + COALESCE(SUM(
                CASE
                    WHEN t.type = 'INCOME' AND t.accountId = a.id THEN t.amountMinor
                    WHEN t.type = 'EXPENSE' AND t.accountId = a.id THEN -t.amountMinor
                    WHEN t.type = 'TRANSFER' AND t.accountId = a.id THEN -t.amountMinor
                    WHEN t.type = 'TRANSFER' AND t.toAccountId = a.id THEN t.amountMinor
                    ELSE 0
                END
            ), 0) AS currentBalanceMinor
        FROM accounts a
        LEFT JOIN transactions t
            ON t.bookId = a.bookId
            AND t.deletedAt IS NULL
            AND (t.accountId = a.id OR t.toAccountId = a.id)
        WHERE a.bookId = :bookId AND a.deletedAt IS NULL
        GROUP BY a.id
        ORDER BY a.sortOrder, a.name
        """,
    )
    fun observeAccountBalances(bookId: String): Flow<List<AccountBalanceItem>>

    @Query(
        """
        SELECT * FROM categories
        WHERE bookId = :bookId AND transactionType = :type AND deletedAt IS NULL
        ORDER BY sortOrder, name
        """,
    )
    fun observeCategories(bookId: String, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM budgets WHERE bookId = :bookId ORDER BY periodStart DESC")
    fun observeBudgets(bookId: String): Flow<List<BudgetEntity>>

    @Query(
        """
        SELECT * FROM budgets
        WHERE bookId = :bookId
          AND scopeKey = :scopeKey
          AND periodStart = :periodStart
          AND periodEnd = :periodEnd
        LIMIT 1
        """,
    )
    suspend fun getBudget(
        bookId: String,
        scopeKey: String,
        periodStart: Long,
        periodEnd: Long,
    ): BudgetEntity?

    @Query("SELECT * FROM savings_plans WHERE bookId = :bookId AND isArchived = 0 ORDER BY createdAt DESC")
    fun observeSavingsPlans(bookId: String): Flow<List<SavingsPlanEntity>>

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
        LEFT JOIN categories c ON c.id = t.categoryId AND c.bookId = t.bookId
        LEFT JOIN accounts a ON a.id = t.accountId AND a.bookId = t.bookId
        WHERE t.bookId = :bookId AND t.deletedAt IS NULL
        ORDER BY t.occurredAt DESC, t.createdAt DESC
        """,
    )
    fun observeTransactions(bookId: String): Flow<List<TransactionListItem>>

    @Query("SELECT * FROM transactions WHERE bookId = :bookId AND id = :id LIMIT 1")
    suspend fun getTransaction(bookId: String, id: String): TransactionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM accounts WHERE bookId = :bookId AND id = :accountId AND deletedAt IS NULL)")
    suspend fun accountExists(bookId: String, accountId: String): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM categories
            WHERE bookId = :bookId AND id = :categoryId
              AND transactionType = :transactionType AND deletedAt IS NULL
        )
        """,
    )
    suspend fun categoryExists(bookId: String, categoryId: String, transactionType: String): Boolean

    @Upsert
    suspend fun upsertBook(book: LedgerBookEntity)

    @Upsert
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Upsert
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Upsert
    suspend fun upsertBudget(budget: BudgetEntity)

    @Upsert
    suspend fun upsertSavingsPlan(plan: SavingsPlanEntity)

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query(
        """
        UPDATE transactions
        SET deletedAt = :deletedAt, updatedAt = :deletedAt
        WHERE bookId = :bookId AND id = :id
        """,
    )
    suspend fun softDeleteTransaction(bookId: String, id: String, deletedAt: Long)

    @Query(
        """
        UPDATE transactions
        SET deletedAt = NULL, updatedAt = :updatedAt
        WHERE bookId = :bookId AND id = :id
        """,
    )
    suspend fun restoreTransaction(bookId: String, id: String, updatedAt: Long)

    @Transaction
    suspend fun seed(
        book: LedgerBookEntity,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        budget: BudgetEntity,
        savingsPlan: SavingsPlanEntity,
    ) {
        upsertBook(book)
        upsertAccounts(accounts)
        upsertCategories(categories)
        upsertBudget(budget)
        upsertSavingsPlan(savingsPlan)
    }
}
