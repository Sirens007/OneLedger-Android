package com.oneledger.app.ui.screens

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.semantics
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
import com.oneledger.app.util.ChineseCalendarLabel
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.MonthWindow
import com.oneledger.app.util.calendarDateCells
import com.oneledger.app.util.chineseCalendarLabel
import com.oneledger.app.util.clampedDayStart
import com.oneledger.app.util.dayLabel
import com.oneledger.app.util.localDayOfMonth
import com.oneledger.app.util.monthLabel
import com.oneledger.app.util.startOfLocalDay
import com.oneledger.app.util.timeLabel
import java.text.DecimalFormat
import java.util.Collections
import java.util.LinkedHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CalendarPageCount = Int.MAX_VALUE
private const val CalendarInitialPage = CalendarPageCount / 2
private const val CalendarMonthCacheSize = 5
private val CalendarPagerHeight = 454.dp
private val WeekdayLabels = listOf("日", "一", "二", "三", "四", "五", "六")
private val CalendarAmountFormat = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue(): DecimalFormat = DecimalFormat("0.#")
}

private data class CalendarDaySummary(
    val expenseMinor: Long,
    val incomeMinor: Long,
)

private data class CalendarDataIndex(
    val transactionsByDay: Map<Long, List<TransactionListItem>>,
    val summariesByDay: Map<Long, CalendarDaySummary>,
) {
    companion object {
        val Empty = CalendarDataIndex(emptyMap(), emptyMap())
    }
}

private data class CalendarMonthUiState(
    val window: MonthWindow,
    val cells: List<CalendarDateCell>,
    val chineseLabels: Map<Long, ChineseCalendarLabel>,
)

private val calendarMonthCache = Collections.synchronizedMap(
    object : LinkedHashMap<Long, CalendarMonthUiState>(CalendarMonthCacheSize, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Long, CalendarMonthUiState>?,
        ): Boolean = size > CalendarMonthCacheSize
    },
)

@Composable
fun IncomeExpenseCalendarScreen(
    state: OneLedgerUiState,
    onBack: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long? = null,
    initialSelectedDayStart: Long? = null,
    initialMonthOffset: Int = 0,
    onTransactionClick: (String) -> Unit = {},
) {
    // Capture "now" once for a screen session. A default expression calling currentTimeMillis()
    // would otherwise remap every pager page whenever the parent recomposes.
    val calendarAnchorMillis = remember(nowMillis) { nowMillis ?: System.currentTimeMillis() }
    val initialPage = remember(initialMonthOffset) {
        (CalendarInitialPage + initialMonthOffset).coerceIn(0, CalendarPageCount - 1)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { CalendarPageCount },
    )
    val scope = rememberCoroutineScope()
    val inspectionMode = LocalInspectionMode.current
    val initialMonthWindow = remember(initialMonthOffset, calendarAnchorMillis) {
        MonthWindow.offset(initialMonthOffset, calendarAnchorMillis)
    }
    val requestedStart = remember(initialSelectedDayStart) {
        initialSelectedDayStart?.startOfLocalDay()
    }
    val initialPreferredDay = remember(requestedStart, calendarAnchorMillis) {
        requestedStart?.localDayOfMonth() ?: calendarAnchorMillis.localDayOfMonth()
    }
    var preferredDayOfMonth by rememberSaveable {
        mutableIntStateOf(initialPreferredDay)
    }
    var selectedDayStart by rememberSaveable {
        mutableLongStateOf(
            if (requestedStart != null && requestedStart >= initialMonthWindow.start &&
                requestedStart < initialMonthWindow.endExclusive
            ) {
                requestedStart
            } else {
                initialMonthWindow.clampedDayStart(initialPreferredDay)
            },
        )
    }
    val initialCalendarData = remember(state.transactions, inspectionMode) {
        if (inspectionMode) buildCalendarDataIndex(state.transactions) else CalendarDataIndex.Empty
    }
    val calendarData by produceState(
        initialValue = initialCalendarData,
        key1 = state.transactions,
    ) {
        if (!inspectionMode) {
            value = withContext(Dispatchers.Default) {
                buildCalendarDataIndex(state.transactions)
            }
        }
    }

    LaunchedEffect(pagerState, calendarAnchorMillis) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                val window = MonthWindow.offset(
                    monthOffset = settledPage - CalendarInitialPage,
                    now = calendarAnchorMillis,
                )
                selectedDayStart = window.clampedDayStart(preferredDayOfMonth)
            }
    }

    val visibleMonthOffset = pagerState.currentPage - CalendarInitialPage
    val visibleMonthWindow = remember(visibleMonthOffset, calendarAnchorMillis) {
        MonthWindow.offset(visibleMonthOffset, calendarAnchorMillis)
    }
    val selectedTransactions = calendarData.transactionsByDay[selectedDayStart].orEmpty()

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
        CalendarMonthHeader(
            label = visibleMonthWindow.monthLabel(),
            onPrevious = {
                scope.launch {
                    val anchor = if (pagerState.isScrollInProgress) {
                        pagerState.targetPage
                    } else {
                        pagerState.currentPage
                    }
                    pagerState.animateScrollToPage((anchor - 1).coerceAtLeast(0))
                }
            },
            onNext = {
                scope.launch {
                    val anchor = if (pagerState.isScrollInProgress) {
                        pagerState.targetPage
                    } else {
                        pagerState.currentPage
                    }
                    pagerState.animateScrollToPage(
                        (anchor + 1).coerceAtMost(CalendarPageCount - 1),
                    )
                }
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CalendarPagerHeight)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { it },
                verticalAlignment = Alignment.Top,
            ) { page ->
                val monthOffset = page - CalendarInitialPage
                val window = remember(monthOffset, calendarAnchorMillis) {
                    MonthWindow.offset(monthOffset, calendarAnchorMillis)
                }
                val initialMonthState = remember(window.start, inspectionMode) {
                    cachedCalendarMonth(window.start) ?: calendarMonthSkeleton(
                        window = window,
                        includeChineseLabels = inspectionMode,
                    )
                }
                val monthUiState by produceState(
                    initialValue = initialMonthState,
                    key1 = window.start,
                ) {
                    if (!inspectionMode && value.chineseLabels.isEmpty()) {
                        value = cachedCalendarMonth(window.start) ?: loadCalendarMonth(
                            window = window,
                            cells = value.cells,
                        )
                    }
                }
                val selectedForPage = window.clampedDayStart(preferredDayOfMonth)

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            RoundedCornerShape(26.dp),
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(26.dp),
                ) {
                    CalendarGrid(
                        cells = monthUiState.cells,
                        summariesByDay = calendarData.summariesByDay,
                        chineseLabels = monthUiState.chineseLabels,
                        selectedDayStart = selectedForPage,
                        onDaySelected = { dayStart ->
                            preferredDayOfMonth = dayStart.localDayOfMonth()
                            selectedDayStart = dayStart
                        },
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        ) {
            item(key = "selected-day-transactions") {
                SelectedDayTransactionContent(
                    dayStart = selectedDayStart,
                    transactions = selectedTransactions,
                    onTransactionClick = onTransactionClick,
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
                "账本 ▼",
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
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarArrow(Icons.Default.ChevronLeft, "上个月", onPrevious)
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        CalendarArrow(Icons.Default.ChevronRight, "下个月", onNext)
    }
}

@Composable
private fun CalendarArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        color = BrandBlue,
        shape = CircleShape,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(20.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    cells: List<CalendarDateCell>,
    summariesByDay: Map<Long, CalendarDaySummary>,
    chineseLabels: Map<Long, ChineseCalendarLabel>,
    selectedDayStart: Long,
    onDaySelected: (Long) -> Unit,
) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
        WeekdayHeader()
        Spacer(Modifier.height(6.dp))
        repeat(6) { rowIndex ->
            if (rowIndex > 0) Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(7) { columnIndex ->
                    val cell = cells[rowIndex * 7 + columnIndex]
                    key(cell.startMillis) {
                        val summary = summariesByDay[cell.startMillis]
                        CalendarDayCell(
                            cell = cell,
                            chineseLabel = chineseLabels[cell.startMillis],
                            expenseMinor = summary?.expenseMinor ?: 0,
                            incomeMinor = summary?.incomeMinor ?: 0,
                            selected = selectedDayStart == cell.startMillis,
                            onClick = { if (cell.inCurrentMonth) onDaySelected(cell.startMillis) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        CalendarLegend()
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        WeekdayLabels.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("底色表示当日净收支：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.size(6.dp).background(ExpenseCoral, CircleShape))
        Text(" 支出较多", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(12.dp))
        Box(Modifier.size(6.dp).background(IncomeMint, CircleShape))
        Text(" 收入较多", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarDateCell,
    chineseLabel: ChineseCalendarLabel?,
    expenseMinor: Long,
    incomeMinor: Long,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTransactions = expenseMinor > 0 || incomeMinor > 0
    val targetBackground = when {
        !cell.inCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        selected -> BrandBlue.copy(alpha = 0.68f)
        expenseMinor > incomeMinor -> ExpenseCoral.copy(alpha = 0.14f)
        incomeMinor > expenseMinor -> IncomeMint.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
    }
    Surface(
        onClick = onClick,
        enabled = cell.inCurrentMonth,
        modifier = modifier.height(58.dp),
        color = targetBackground,
        shape = RoundedCornerShape(11.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
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
            if (cell.inCurrentMonth && hasTransactions) {
                Text(
                    text = "−${expenseMinor.calendarAmount()}",
                    color = ExpenseCoral,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "+${incomeMinor.calendarAmount()}",
                    color = IncomeMint,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            } else {
                Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = chineseLabel?.text.orEmpty(),
                        color = when {
                            selected -> Color.White.copy(alpha = 0.78f)
                            cell.inCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (chineseLabel?.isFestival == true) 0.86f else 0.70f,
                            )
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                        },
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = if (chineseLabel?.isFestival == true) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDayTransactionContent(
    dayStart: Long,
    transactions: List<TransactionListItem>,
    onTransactionClick: (String) -> Unit,
) {
    val expense = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    }
    val income = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    }
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
                        key(transaction.id) {
                            CalendarTransactionRow(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction.id) },
                            )
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
}

@Composable
private fun CalendarTransactionRow(
    transaction: TransactionListItem,
    onClick: () -> Unit,
) {
    val color = when (transaction.type) {
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryGlyph(
                iconKey = transaction.iconKey,
                color = colorFromLong(transaction.colorHex),
                modifier = Modifier.size(38.dp),
                contentDescription = null,
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
}

private fun buildCalendarDataIndex(
    transactions: List<TransactionListItem>,
): CalendarDataIndex {
    val transactionsByDay = LinkedHashMap<Long, MutableList<TransactionListItem>>()
    val summariesByDay = LinkedHashMap<Long, CalendarDaySummary>()
    transactions.forEach { transaction ->
        val dayStart = transaction.occurredAt.startOfLocalDay()
        transactionsByDay.getOrPut(dayStart) { mutableListOf() }.add(transaction)
        val previous = summariesByDay[dayStart] ?: CalendarDaySummary(0, 0)
        summariesByDay[dayStart] = when (transaction.type) {
            TransactionType.EXPENSE -> previous.copy(
                expenseMinor = previous.expenseMinor + transaction.amountMinor,
            )
            TransactionType.INCOME -> previous.copy(
                incomeMinor = previous.incomeMinor + transaction.amountMinor,
            )
            TransactionType.TRANSFER -> previous
            else -> previous
        }
    }
    return CalendarDataIndex(
        transactionsByDay = transactionsByDay.mapValues { (_, value) -> value.toList() },
        summariesByDay = summariesByDay,
    )
}

private fun cachedCalendarMonth(start: Long): CalendarMonthUiState? =
    synchronized(calendarMonthCache) { calendarMonthCache[start] }

private fun calendarMonthSkeleton(
    window: MonthWindow,
    includeChineseLabels: Boolean,
): CalendarMonthUiState {
    val cells = window.calendarDateCells()
    return CalendarMonthUiState(
        window = window,
        cells = cells,
        chineseLabels = if (includeChineseLabels) {
            cells.associate { cell -> cell.startMillis to cell.startMillis.chineseCalendarLabel() }
        } else {
            emptyMap()
        },
    )
}

private suspend fun loadCalendarMonth(
    window: MonthWindow,
    cells: List<CalendarDateCell>,
): CalendarMonthUiState =
    withContext(Dispatchers.Default) {
        cachedCalendarMonth(window.start) ?: run {
            CalendarMonthUiState(
                window = window,
                cells = cells,
                chineseLabels = cells.associate { cell ->
                    cell.startMillis to cell.startMillis.chineseCalendarLabel()
                },
            ).also { loaded ->
                synchronized(calendarMonthCache) {
                    calendarMonthCache[window.start] = loaded
                }
            }
        }
    }

private fun Long.calendarAmount(): String {
    val major = this / 100.0
    val formatter = requireNotNull(CalendarAmountFormat.get())
    return when {
        major >= 1_000 -> "${formatter.format(major / 1_000)}k"
        else -> formatter.format(major)
    }
}

private fun TransactionListItem.timeLabelSafe(): String = occurredAt.timeLabel()
