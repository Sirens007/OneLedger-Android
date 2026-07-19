package com.oneledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.app.data.local.BudgetEntity
import com.oneledger.app.data.local.TransactionListItem
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.OneLedgerUiState
import com.oneledger.app.ui.components.CategoryGlyph
import com.oneledger.app.ui.components.EmptyLedgerState
import com.oneledger.app.ui.components.OneLedgerCard
import com.oneledger.app.ui.components.PressableSurface
import com.oneledger.app.ui.components.ScreenHeader
import com.oneledger.app.ui.components.colorFromLong
import com.oneledger.app.ui.theme.BrandTeal
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.ui.theme.IncomeMint
import com.oneledger.app.ui.theme.SavingsAmber
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.MonthWindow
import com.oneledger.app.util.dayKey
import com.oneledger.app.util.dayLabel
import com.oneledger.app.util.calendarDateCells
import com.oneledger.app.util.chineseCalendarLabel
import com.oneledger.app.util.isIn
import com.oneledger.app.util.monthLabel
import com.oneledger.app.util.timeLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LedgerScreen(
    state: OneLedgerUiState,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
    onBudgetClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val monthWindow = remember(monthOffset, nowMillis) { MonthWindow.offset(monthOffset, nowMillis) }
    LaunchedEffect(monthOffset, nowMillis) {
        withContext(Dispatchers.Default) {
            (-1..1).forEach { adjacentOffset ->
                MonthWindow.offset(monthOffset + adjacentOffset, nowMillis)
                    .calendarDateCells()
                    .forEach { it.startMillis.chineseCalendarLabel() }
            }
        }
    }
    val monthTransactions = state.transactions.filter { it.occurredAt.isIn(monthWindow) }
    val visibleTransactions = monthTransactions.filter {
        query.isBlank() || it.categoryName.contains(query, ignoreCase = true) ||
            it.note.contains(query, ignoreCase = true) || it.accountName.contains(query, ignoreCase = true)
    }
    val monthExpense = monthTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amountMinor }
    val monthBudget = state.budgets.firstOrNull {
        it.categoryId == null && monthWindow.start >= it.periodStart && monthWindow.start < it.periodEnd
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ScreenHeader(
            title = "账本",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            action = {
                PressableSurface(
                    onClick = {},
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "更多")
                    }
                }
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 118.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                placeholder = { Text("搜索分类、备注或账户") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                ),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(monthWindow.monthLabel(), style = MaterialTheme.typography.headlineMedium)
                MonthButton(
                    icon = Icons.Default.ChevronLeft,
                    description = "上个月",
                    onClick = { monthOffset -= 1 },
                )
                MonthButton(
                    icon = Icons.Default.ChevronRight,
                    description = "下个月",
                    enabled = monthOffset < 0,
                    onClick = { monthOffset += 1 },
                )
                Spacer(Modifier.weight(1f))
                PressableSurface(
                    onClick = onCalendarClick,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "收支日历",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { BudgetHero(budget = monthBudget, spentMinor = monthExpense, onClick = onBudgetClick) }

        if (visibleTransactions.isEmpty()) {
            item {
                OneLedgerCard(Modifier.fillMaxWidth()) {
                    EmptyLedgerState(
                        title = if (query.isBlank()) "本月还没有记录" else "没有匹配的账单",
                        body = if (query.isBlank()) "点右下角 +，记下第一笔收支" else "换个关键词试试",
                    )
                }
            }
        } else {
            val groups = visibleTransactions.groupBy { it.occurredAt.dayKey() }.entries.toList()
            items(groups, key = { it.key }) { group ->
                DailyTransactionGroup(group.value, onTransactionClick)
            }
        }
        }
    }
}

@Composable
private fun MonthButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .padding(start = 8.dp)
            .size(32.dp),
        shape = CircleShape,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BudgetHero(
    budget: BudgetEntity?,
    spentMinor: Long,
    onClick: () -> Unit,
) {
    val limit = budget?.limitMinor ?: 0
    val remaining = (limit - spentMinor).coerceAtLeast(0)
    val dailyRemaining = remaining / 15
    val progress = if (limit <= 0) 0f else (spentMinor.toFloat() / limit).coerceIn(0f, 1f)

    PressableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(26.dp)
                        .background(BrandTeal, CircleShape),
                )
                Text(
                    "本月可用",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandTeal,
                )
                Spacer(Modifier.weight(1f))
                Text("预算轨道", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                MoneyFormatter.format(remaining),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "总预算  ${MoneyFormatter.format(limit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "剩余日均  ${MoneyFormatter.format(dailyRemaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = BrandTeal,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DailyTransactionGroup(
    transactions: List<TransactionListItem>,
    onTransactionClick: (String) -> Unit,
) {
    val dayExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                transactions.first().occurredAt.dayLabel(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "支出 ${MoneyFormatter.format(dayExpense)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OneLedgerCard(Modifier.fillMaxWidth()) {
            Column {
                transactions.forEachIndexed { index, transaction ->
                    TransactionRow(transaction, onClick = { onTransactionClick(transaction.id) })
                    if (index != transactions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 80.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionListItem,
    onClick: () -> Unit,
) {
    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> ExpenseCoral
        TransactionType.INCOME -> IncomeMint
        else -> SavingsAmber
    }
    val sign = if (transaction.type == TransactionType.EXPENSE) -1 else 1
    PressableSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryGlyph(
                iconKey = transaction.iconKey,
                color = colorFromLong(transaction.colorHex),
                contentDescription = null,
            )
            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f),
            ) {
                Text(transaction.categoryName, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(transaction.occurredAt.timeLabel())
                        if (transaction.note.isNotBlank()) append(" · ${transaction.note}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    MoneyFormatter.format(transaction.amountMinor * sign),
                    style = MaterialTheme.typography.titleLarge,
                    color = amountColor,
                    textAlign = TextAlign.End,
                )
                Text(
                    transaction.accountName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
