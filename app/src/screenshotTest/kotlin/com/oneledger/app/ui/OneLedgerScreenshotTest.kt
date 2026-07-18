package com.oneledger.app.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.data.local.BudgetEntity
import com.oneledger.app.data.local.CategoryEntity
import com.oneledger.app.data.local.SavingsPlanEntity
import com.oneledger.app.data.local.TransactionListItem
import com.oneledger.app.domain.model.AccountType
import com.oneledger.app.domain.model.SavingsMethod
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.theme.OneLedgerTheme
import com.oneledger.app.ui.screens.BudgetEditorPreviewSurface
import com.oneledger.app.util.MonthWindow
import java.util.Calendar

private const val PHONE = "spec:width=393dp,height=852dp,dpi=440"
private const val PREVIEW_NOW = 1_784_379_780_000L

@PreviewTest
@Preview(name = "账本 · 深色", device = PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun LedgerScreenshot() = PreviewFrame(Destination.LEDGER)

@PreviewTest
@Preview(name = "资产 · 深色", device = PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun AssetsScreenshot() = PreviewFrame(Destination.ASSETS)

@PreviewTest
@Preview(name = "存钱 · 深色", device = PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun SavingsScreenshot() = PreviewFrame(Destination.SAVINGS)

@PreviewTest
@Preview(name = "统计 · 深色", device = PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun StatisticsScreenshot() = PreviewFrame(Destination.STATISTICS)

@PreviewTest
@Preview(name = "预算详情 · 深色", device = PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun BudgetDetailsScreenshot() = PreviewLedgerPage(LedgerPage.BUDGET)

@PreviewTest
@Preview(name = "预算编辑 · 深色", device = PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun BudgetEditorScreenshot() {
    OneLedgerTheme(darkTheme = true) {
        BudgetEditorPreviewSurface()
    }
}

@PreviewTest
@Preview(name = "收支日历 · 深色", device = PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Composable
fun IncomeExpenseCalendarScreenshot() = PreviewLedgerPage(LedgerPage.CALENDAR)

@Composable
private fun PreviewFrame(destination: Destination) {
    OneLedgerTheme(darkTheme = true) {
        OneLedgerFrame(
            state = previewState(),
            destination = destination,
            onDestinationSelected = {},
            onQuickAdd = {},
            nowMillis = PREVIEW_NOW,
        )
    }
}

@Composable
private fun PreviewLedgerPage(page: LedgerPage) {
    OneLedgerTheme(darkTheme = true) {
        OneLedgerFrame(
            state = previewState(),
            destination = Destination.LEDGER,
            ledgerPage = page,
            onDestinationSelected = {},
            onLedgerPageChanged = {},
            onQuickAdd = {},
            nowMillis = PREVIEW_NOW,
        )
    }
}

private fun previewState(): OneLedgerUiState {
    val now = PREVIEW_NOW
    val month = MonthWindow.current(now)
    val bookId = "book-preview"
    val accounts = listOf(
        AccountEntity(
            id = "daily-card",
            bookId = bookId,
            name = "日常卡",
            subtitle = "储蓄账户",
            type = AccountType.BANK,
            colorHex = 0xFF4D7CFE,
            openingBalanceMinor = 328_600,
            includeInNetWorth = true,
            sortOrder = 0,
            createdAt = now,
            updatedAt = now,
        ),
        AccountEntity(
            id = "cash",
            bookId = bookId,
            name = "现金",
            subtitle = "随身钱包",
            type = AccountType.CASH,
            colorHex = 0xFFFFC247,
            openingBalanceMinor = 86_500,
            includeInNetWorth = true,
            sortOrder = 1,
            createdAt = now,
            updatedAt = now,
        ),
        AccountEntity(
            id = "credit",
            bookId = bookId,
            name = "信用卡",
            subtitle = "本期应还",
            type = AccountType.CREDIT,
            colorHex = 0xFFFF6B5F,
            openingBalanceMinor = -42_600,
            includeInNetWorth = true,
            sortOrder = 2,
            createdAt = now,
            updatedAt = now,
        ),
    )

    val categories = listOf(
        category("food", "餐饮", "food", 0xFFFF6B5F, now, bookId),
        category("transport", "交通", "transport", 0xFF4D7CFE, now, bookId),
        category("shopping", "购物", "shopping", 0xFFFFA94D, now, bookId),
        category("home", "居家", "home", 0xFF57D3A2, now, bookId),
    )

    val transactions = listOf(
        transaction("t1", "餐饮", "food", 0xFFFF6B5F, 2_680, "daily-card", "日常卡", "咖啡和早餐", daysAgo(now, 0)),
        transaction("t2", "交通", "transport", 0xFF4D7CFE, 1_400, "cash", "现金", "地铁", daysAgo(now, 0) - 3_600_000),
        transaction("t3", "购物", "shopping", 0xFFFFA94D, 18_900, "credit", "信用卡", "生活用品", daysAgo(now, 1)),
        transaction("t4", "居家", "home", 0xFF57D3A2, 7_500, "daily-card", "日常卡", "网络服务", daysAgo(now, 3)),
        TransactionListItem(
            id = "t5",
            type = TransactionType.INCOME,
            amountMinor = 520_000,
            categoryId = "salary",
            categoryName = "工资",
            iconKey = "salary",
            colorHex = 0xFF57D3A2,
            accountId = "daily-card",
            accountName = "日常卡",
            toAccountId = null,
            note = "七月工资",
            merchant = "",
            occurredAt = daysAgo(now, 5),
        ),
    )

    return OneLedgerUiState(
        accounts = accounts,
        transactions = transactions,
        budgets = listOf(
            BudgetEntity(
                id = "budget-preview",
                bookId = bookId,
                periodStart = month.start,
                periodEnd = month.endExclusive,
                limitMinor = 500_000,
                createdAt = now,
                updatedAt = now,
            ),
            BudgetEntity(
                id = "budget-preview-food",
                bookId = bookId,
                categoryId = "food",
                periodStart = month.start,
                periodEnd = month.endExclusive,
                limitMinor = 100_000,
                createdAt = now,
                updatedAt = now,
            ),
            BudgetEntity(
                id = "budget-preview-transport",
                bookId = bookId,
                categoryId = "transport",
                periodStart = month.start,
                periodEnd = month.endExclusive,
                limitMinor = 80_000,
                createdAt = now,
                updatedAt = now,
            ),
        ),
        savingsPlans = listOf(
            SavingsPlanEntity(
                id = "plan-travel",
                bookId = bookId,
                name = "去看海",
                method = SavingsMethod.FLEXIBLE,
                targetMinor = 800_000,
                savedMinor = 356_000,
                colorHex = 0xFF57D3A2,
                startAt = daysAgo(now, 45),
                endAt = daysFrom(now, 120),
                isArchived = false,
                createdAt = now,
                updatedAt = now,
            ),
            SavingsPlanEntity(
                id = "plan-emergency",
                bookId = bookId,
                name = "应急备用金",
                method = SavingsMethod.FIXED,
                targetMinor = 2_000_000,
                savedMinor = 1_280_000,
                colorHex = 0xFF9B7BFF,
                startAt = daysAgo(now, 180),
                endAt = daysFrom(now, 185),
                isArchived = false,
                createdAt = now - 1,
                updatedAt = now,
            ),
        ),
        expenseCategories = categories,
        incomeCategories = listOf(category("salary", "工资", "salary", 0xFF57D3A2, now, bookId, TransactionType.INCOME)),
    )
}

private fun category(
    id: String,
    name: String,
    icon: String,
    color: Long,
    now: Long,
    bookId: String,
    type: String = TransactionType.EXPENSE,
) = CategoryEntity(
    id = id,
    bookId = bookId,
    name = name,
    transactionType = type,
    iconKey = icon,
    colorHex = color,
    sortOrder = 0,
    isSystem = true,
    createdAt = now,
    updatedAt = now,
)

private fun transaction(
    id: String,
    category: String,
    icon: String,
    color: Long,
    amount: Long,
    accountId: String,
    accountName: String,
    note: String,
    occurredAt: Long,
) = TransactionListItem(
    id = id,
    type = TransactionType.EXPENSE,
    amountMinor = amount,
    categoryId = icon,
    categoryName = category,
    iconKey = icon,
    colorHex = color,
    accountId = accountId,
    accountName = accountName,
    toAccountId = null,
    note = note,
    merchant = "",
    occurredAt = occurredAt,
)

private fun daysAgo(now: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = now
    add(Calendar.DAY_OF_YEAR, -days)
}.timeInMillis

private fun daysFrom(now: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = now
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis
