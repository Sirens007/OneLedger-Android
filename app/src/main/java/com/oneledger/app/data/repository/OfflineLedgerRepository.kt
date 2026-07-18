package com.oneledger.app.data.repository

import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.data.local.BudgetEntity
import com.oneledger.app.data.local.CategoryEntity
import com.oneledger.app.data.local.LedgerBookEntity
import com.oneledger.app.data.local.LedgerDao
import com.oneledger.app.data.local.SavingsPlanEntity
import com.oneledger.app.data.local.TransactionEntity
import com.oneledger.app.domain.model.AccountType
import com.oneledger.app.domain.model.NewTransaction
import com.oneledger.app.domain.model.SavingsMethod
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.util.MonthWindow
import java.util.Calendar
import java.util.UUID

class OfflineLedgerRepository(
    private val dao: LedgerDao,
) : LedgerRepository {
    override val accounts = dao.observeAccounts()
    override val transactions = dao.observeTransactions()
    override val budgets = dao.observeBudgets()
    override val savingsPlans = dao.observeSavingsPlans()

    override fun categories(type: String) = dao.observeCategories(type)

    override suspend fun ensureSeedData() {
        if (dao.countBooks() > 0) return

        val now = System.currentTimeMillis()
        val month = MonthWindow.current()
        val yearLater = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.YEAR, 1)
        }.timeInMillis

        val book = LedgerBookEntity(
            id = DEFAULT_BOOK_ID,
            name = "日常账本",
            currencyCode = "CNY",
            isDefault = true,
            createdAt = now,
            updatedAt = now,
        )

        val accounts = listOf(
            AccountEntity(
                id = "account-cash",
                bookId = DEFAULT_BOOK_ID,
                name = "现金",
                subtitle = "现金钱包",
                type = AccountType.CASH,
                colorHex = 0xFFFFC247,
                openingBalanceMinor = 0,
                includeInNetWorth = true,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
            ),
            AccountEntity(
                id = "account-daily",
                bookId = DEFAULT_BOOK_ID,
                name = "日常卡",
                subtitle = "储蓄账户",
                type = AccountType.BANK,
                colorHex = 0xFF4D7CFE,
                openingBalanceMinor = 0,
                includeInNetWorth = true,
                sortOrder = 1,
                createdAt = now,
                updatedAt = now,
            ),
        )

        val categories = expenseCategories(now) + incomeCategories(now)

        dao.seed(
            book = book,
            accounts = accounts,
            categories = categories,
            budget = BudgetEntity(
                id = "budget-${month.start}",
                bookId = DEFAULT_BOOK_ID,
                periodStart = month.start,
                periodEnd = month.endExclusive,
                limitMinor = 500_000,
                createdAt = now,
                updatedAt = now,
            ),
            savingsPlan = SavingsPlanEntity(
                id = "savings-first-fund",
                bookId = DEFAULT_BOOK_ID,
                name = "第一桶金",
                method = SavingsMethod.FLEXIBLE,
                targetMinor = 1_000_000,
                savedMinor = 0,
                colorHex = 0xFF57D3A2,
                startAt = now,
                endAt = yearLater,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun addTransaction(transaction: NewTransaction): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        dao.insertTransaction(
            TransactionEntity(
                id = id,
                bookId = DEFAULT_BOOK_ID,
                type = transaction.type,
                amountMinor = transaction.amountMinor,
                categoryId = transaction.categoryId,
                accountId = transaction.accountId,
                toAccountId = transaction.toAccountId,
                note = transaction.note.trim(),
                occurredAt = transaction.occurredAt,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    override suspend fun undoTransaction(id: String) {
        dao.softDeleteTransaction(id, System.currentTimeMillis())
    }

    override suspend fun saveBudget(
        periodStart: Long,
        periodEnd: Long,
        limitMinor: Long,
        categoryId: String?,
    ) {
        require(periodEnd > periodStart) { "预算周期必须有效" }
        require(limitMinor >= 0) { "预算金额不能为负数" }
        val now = System.currentTimeMillis()
        val suffix = categoryId ?: "total"
        dao.insertBudget(
            BudgetEntity(
                id = if (categoryId == null) "budget-$periodStart" else "budget-$periodStart-$suffix",
                bookId = DEFAULT_BOOK_ID,
                categoryId = categoryId,
                periodStart = periodStart,
                periodEnd = periodEnd,
                limitMinor = limitMinor,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private fun expenseCategories(now: Long) = listOf(
        category("expense-food", "餐饮", TransactionType.EXPENSE, "food", 0xFFFF6B5F, 0, now),
        category("expense-transport", "交通", TransactionType.EXPENSE, "transport", 0xFF4D7CFE, 1, now),
        category("expense-shopping", "购物", TransactionType.EXPENSE, "shopping", 0xFFFFA94D, 2, now),
        category("expense-fun", "娱乐", TransactionType.EXPENSE, "movie", 0xFF9B7BFF, 3, now),
        category("expense-home", "居家", TransactionType.EXPENSE, "home", 0xFF57D3A2, 4, now),
        category("expense-study", "学习", TransactionType.EXPENSE, "study", 0xFF4CC9F0, 5, now),
        category("expense-health", "医疗", TransactionType.EXPENSE, "health", 0xFFFF7AA2, 6, now),
        category("expense-other", "其他", TransactionType.EXPENSE, "other", 0xFF8992A3, 7, now),
    )

    private fun incomeCategories(now: Long) = listOf(
        category("income-salary", "工资", TransactionType.INCOME, "salary", 0xFF57D3A2, 0, now),
        category("income-bonus", "奖金", TransactionType.INCOME, "bonus", 0xFFFFC247, 1, now),
        category("income-invest", "理财", TransactionType.INCOME, "invest", 0xFF4D7CFE, 2, now),
        category("income-other", "其他", TransactionType.INCOME, "other", 0xFF8992A3, 3, now),
    )

    private fun category(
        id: String,
        name: String,
        type: String,
        icon: String,
        color: Long,
        order: Int,
        now: Long,
    ) = CategoryEntity(
        id = id,
        bookId = DEFAULT_BOOK_ID,
        name = name,
        transactionType = type,
        iconKey = icon,
        colorHex = color,
        sortOrder = order,
        isSystem = true,
        createdAt = now,
        updatedAt = now,
    )

    companion object {
        const val DEFAULT_BOOK_ID = "book-default"
    }
}
