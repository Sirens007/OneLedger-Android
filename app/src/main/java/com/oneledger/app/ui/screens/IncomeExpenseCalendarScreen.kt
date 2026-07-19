package com.oneledger.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.app.data.local.TransactionListItem
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.OneLedgerUiState
import com.oneledger.app.ui.components.CategoryGlyph
import com.oneledger.app.ui.components.OneLedgerCard
import com.oneledger.app.ui.components.PressableSurface
import com.oneledger.app.ui.components.colorFromLong
import com.oneledger.app.ui.theme.BrandBlue
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.ui.theme.IncomeMint
import com.oneledger.app.ui.theme.SavingsAmber
import com.oneledger.app.util.CalendarDateCell
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.MonthWindow
import com.oneledger.app.util.calendarDateCells
import com.oneledger.app.util.dayKey
import com.oneledger.app.util.dayLabel
import com.oneledger.app.util.isIn
import com.oneledger.app.util.monthLabel
import com.oneledger.app.util.nextLocalDayStart
import com.oneledger.app.util.startOfLocalDay
import com.oneledger.app.util.timeLabel
import java.text.DecimalFormat

@Composable
fun IncomeExpenseCalendarScreen(
    state: OneLedgerUiState,
    onBack: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
    initialSelectedDayStart: Long? = null,
) {
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val monthWindow = remember(monthOffset, nowMillis) { MonthWindow.offset(monthOffset, nowMillis) }
    var selectedDayStart by rememberSaveable(monthWindow.start, initialSelectedDayStart) {
        val requestedDay = initialSelectedDayStart?.startOfLocalDay()
        mutableLongStateOf(
            when {
                requestedDay != null && requestedDay.isIn(monthWindow) -> requestedDay
                nowMillis.isIn(monthWindow) -> nowMillis.startOfLocalDay()
                else -> monthWindow.start
            },
        )
    }
    val cells = remember(monthWindow) { monthWindow.calendarDateCells() }
    val transactionsByDay = state.transactions.groupBy { it.occurredAt.dayKey() }
    val selectedTransactions = state.transactions.filter {
        it.occurredAt >= selectedDayStart && it.occurredAt < selectedDayStart.nextLocalDayStart()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        CalendarTopBar(
            onBack = onBack,
            onQuickAdd = onQuickAdd,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CalendarMonthHeader(
                    label = monthWindow.monthLabel(),
                    canMoveForward = monthOffset < 0,
                    onPrevious = { monthOffset -= 1 },
                    onNext = { monthOffset += 1 },
                )
            }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            RoundedCornerShape(26.dp),
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(26.dp),
                ) {
                    CalendarGrid(
                        cells = cells,
                        transactionsByDay = transactionsByDay,
                        selectedDayStart = selectedDayStart,
                        onDaySelected = { selectedDayStart = it },
                    )
                }
            }
            item {
                SelectedDayTransactions(
                    dayStart = selectedDayStart,
                    transactions = selectedTransactions,
                )
            }
        }
    }
}

@Composable
private fun CalendarTopBar(
    onBack: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressableSurface(
            onClick = onBack,
            modifier = Modifier.size(44.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(15.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", modifier = Modifier.size(23.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(15.dp),
        ) {
            Text(
                "账本 ▾",
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.weight(1f))
        PressableSurface(
            onClick = onQuickAdd,
            modifier = Modifier.size(44.dp),
            color = BrandBlue,
            shape = RoundedCornerShape(15.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "记一笔", tint = Color.White, modifier = Modifier.size(25.dp))
            }
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    label: String,
    canMoveForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarArrow(Icons.Default.ChevronLeft, "上个月", true, onPrevious)
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        CalendarArrow(Icons.Default.ChevronRight, "下个月", canMoveForward, onNext)
    }
}

@Composable
private fun CalendarArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
        color = BrandBlue.copy(alpha = if (enabled) 1f else 0.14f),
        shape = CircleShape,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = if (enabled) 1f else 0.32f),
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    cells: List<CalendarDateCell>,
    transactionsByDay: Map<String, List<TransactionListItem>>,
    selectedDayStart: Long,
    onDaySelected: (Long) -> Unit,
) {
    Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        cells.chunked(7).forEachIndexed { rowIndex, week ->
            if (rowIndex > 0) Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { cell ->
                    val transactions = transactionsByDay[cell.startMillis.dayKey()].orEmpty()
                    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
                    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
                    CalendarDayCell(
                        cell = cell,
                        expenseMinor = expense,
                        incomeMinor = income,
                        selected = selectedDayStart == cell.startMillis,
                        onClick = { if (cell.inCurrentMonth) onDaySelected(cell.startMillis) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("底色表示当日净收支：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.size(6.dp).background(ExpenseCoral, CircleShape))
            Text(" 支出较多", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(12.dp))
            Box(Modifier.size(6.dp).background(IncomeMint, CircleShape))
            Text(" 收入较多", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarDateCell,
    expenseMinor: Long,
    incomeMinor: Long,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetBackground = when {
        !cell.inCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        selected -> BrandBlue.copy(alpha = 0.68f)
        expenseMinor > incomeMinor -> ExpenseCoral.copy(alpha = 0.14f)
        incomeMinor > expenseMinor -> IncomeMint.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(140),
        label = "calendar-selection",
    )
    PressableSurface(
        onClick = onClick,
        enabled = cell.inCurrentMonth,
        modifier = modifier.height(66.dp),
        color = background,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    cell.dayNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        selected -> Color.White
                        cell.inCurrentMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                    },
                )
            }
            Text(
                text = if (cell.inCurrentMonth) "−${expenseMinor.calendarAmount()}" else "",
                color = ExpenseCoral,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = if (cell.inCurrentMonth) "+${incomeMinor.calendarAmount()}" else "",
                color = IncomeMint,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedDayTransactions(
    dayStart: Long,
    transactions: List<TransactionListItem>,
) {
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dayStart.dayLabel(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Text(
                "−${MoneyFormatter.format(expense)}  +${MoneyFormatter.format(income)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OneLedgerCard(Modifier.fillMaxWidth()) {
            if (transactions.isEmpty()) {
                Text(
                    "这一天还没有收支记录",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    transactions.forEachIndexed { index, transaction ->
                        CalendarTransactionRow(transaction)
                        if (index != transactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 70.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTransactionRow(transaction: TransactionListItem) {
    val color = when (transaction.type) {
        TransactionType.EXPENSE -> ExpenseCoral
        TransactionType.INCOME -> IncomeMint
        else -> SavingsAmber
    }
    val sign = if (transaction.type == TransactionType.EXPENSE) -1 else 1
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryGlyph(
            iconKey = transaction.iconKey,
            color = colorFromLong(transaction.colorHex),
            modifier = Modifier.size(38.dp),
            contentDescription = transaction.categoryName,
        )
        Column(
            modifier = Modifier
                .padding(start = 11.dp)
                .weight(1f),
        ) {
            Text(transaction.categoryName, style = MaterialTheme.typography.titleMedium)
            Text(
                listOf(transaction.timeLabelSafe(), transaction.note).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(MoneyFormatter.format(transaction.amountMinor * sign), color = color, style = MaterialTheme.typography.titleMedium)
            Text(transaction.accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Long.calendarAmount(): String {
    val major = this / 100.0
    return when {
        major >= 1_000 -> DecimalFormat("0.#k").format(major / 1_000)
        else -> DecimalFormat("0.#").format(major)
    }
}

private fun TransactionListItem.timeLabelSafe(): String = occurredAt.timeLabel()
