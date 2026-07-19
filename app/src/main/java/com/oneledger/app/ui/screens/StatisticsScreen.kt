package com.oneledger.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.oneledger.app.data.local.TransactionListItem
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.OneLedgerUiState
import com.oneledger.app.ui.components.LabelValue
import com.oneledger.app.ui.components.OneLedgerCard
import com.oneledger.app.ui.components.PressableSurface
import com.oneledger.app.ui.components.ScreenHeader
import com.oneledger.app.ui.components.SectionTitle
import com.oneledger.app.ui.components.colorFromLong
import com.oneledger.app.ui.theme.BrandBlue
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.ui.theme.IncomeMint
import com.oneledger.app.ui.theme.OneLedgerMotion
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.MonthWindow
import java.util.Calendar

private enum class StatsRange(val label: String) {
    WEEK("周"), MONTH("月"), YEAR("年"), ALL("全部")
}

private data class CategoryTotal(
    val name: String,
    val amountMinor: Long,
    val color: Color,
)

@Composable
fun StatisticsScreen(
    state: OneLedgerUiState,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    var selectedRange by rememberSaveable { mutableStateOf(StatsRange.MONTH) }
    val listState = rememberLazyListState()
    val now = remember(nowMillis) { nowMillis }
    val start = remember(selectedRange) { selectedRange.startMillis(now) }
    val transactions = state.transactions.filter { it.occurredAt >= start }
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }
    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
    val days = selectedRange.dayDivisor(now).coerceAtLeast(1)
    val categoryTotals = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryName }
        .map { (_, items) ->
            CategoryTotal(
                name = items.first().categoryName,
                amountMinor = items.sumOf { it.amountMinor },
                color = colorFromLong(items.first().colorHex),
            )
        }
        .sortedByDescending { it.amountMinor }

    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ScreenHeader(
            title = "统计",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            action = {
                PressableSurface(
                    onClick = {},
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                }
            },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

        item { RangeSelector(selectedRange, onSelected = { selectedRange = it }) }

        item {
            OneLedgerCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("资金脉冲", trailing = selectedRange.rangeLabel(now))
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth()) {
                        DonutChart(
                            categories = categoryTotals,
                            modifier = Modifier.size(110.dp),
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LabelValue("支出", MoneyFormatter.format(expense), valueColor = ExpenseCoral)
                            LabelValue("收入", MoneyFormatter.format(income), valueColor = IncomeMint)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("结余", MoneyFormatter.format(income - expense), BrandBlue, Modifier.weight(1f))
                    MetricCard("日均支出", MoneyFormatter.format(expense / days), ExpenseCoral, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("最大支出", MoneyFormatter.format(transactions.maxExpense()), ExpenseCoral, Modifier.weight(1f))
                    MetricCard("记录笔数", "${transactions.size} 笔", IncomeMint, Modifier.weight(1f))
                }
            }
        }

        item { SectionTitle("分类轨迹", trailing = "${categoryTotals.size} 类") }

        item {
            OneLedgerCard(Modifier.fillMaxWidth()) {
                if (categoryTotals.isEmpty()) {
                    Text(
                        "这个范围内还没有支出记录。",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        val max = categoryTotals.maxOf { it.amountMinor }.coerceAtLeast(1)
                        categoryTotals.forEach { category ->
                            CategoryBar(category, max)
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun RangeSelector(selected: StatsRange, onSelected: (StatsRange) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(3.dp),
    ) {
        val itemWidth = maxWidth / StatsRange.entries.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * StatsRange.entries.indexOf(selected),
            animationSpec = spring(
                dampingRatio = OneLedgerMotion.NoBounceDamping,
                stiffness = OneLedgerMotion.NavigationStiffness,
            ),
            label = "stats-range-indicator",
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(itemWidth)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(15.dp)),
        )
        Row(Modifier.fillMaxWidth()) {
            StatsRange.entries.forEach { range ->
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.97f else 1f,
                    animationSpec = if (pressed) {
                        snap()
                    } else {
                        spring(
                            dampingRatio = OneLedgerMotion.NoBounceDamping,
                            stiffness = OneLedgerMotion.PressStiffness,
                        )
                    },
                    label = "stats-range-press",
                )
                val content by animateColorAsState(
                    targetValue = if (range == selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(OneLedgerMotion.SelectionMillis),
                    label = "stats-range-content",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .semantics { this.selected = range == selected }
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                        ) { onSelected(range) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        range.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(categories: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val total = categories.sumOf { it.amountMinor }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 13.dp.toPx()
            if (total <= 0) {
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            } else {
                var start = -90f
                categories.forEach { category ->
                    val sweep = category.amountMinor.toFloat() / total * 360f
                    drawArc(
                        color = category.color,
                        startAngle = start,
                        sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("总支出", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(MoneyFormatter.format(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier) {
    OneLedgerCard(modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

@Composable
private fun CategoryBar(category: CategoryTotal, max: Long) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(category.color, CircleShape))
                Text(category.name, modifier = Modifier.padding(start = 9.dp), style = MaterialTheme.typography.titleMedium)
            }
            Text(MoneyFormatter.format(category.amountMinor), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { category.amountMinor.toFloat() / max },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = category.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

private fun List<TransactionListItem>.maxExpense(): Long =
    filter { it.type == TransactionType.EXPENSE }.maxOfOrNull { it.amountMinor } ?: 0

private fun StatsRange.startMillis(now: Long): Long = when (this) {
    StatsRange.WEEK -> Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -6)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    StatsRange.MONTH -> MonthWindow.current(now).start
    StatsRange.YEAR -> Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    StatsRange.ALL -> Long.MIN_VALUE
}

private fun StatsRange.dayDivisor(now: Long): Long = when (this) {
    StatsRange.WEEK -> 7
    StatsRange.MONTH -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toLong()
    StatsRange.YEAR -> Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toLong()
    StatsRange.ALL -> 30
}

private fun StatsRange.rangeLabel(now: Long): String = when (this) {
    StatsRange.WEEK -> "最近 7 天"
    StatsRange.MONTH -> "本月"
    StatsRange.YEAR -> "${Calendar.getInstance().get(Calendar.YEAR)} 年"
    StatsRange.ALL -> "全部时间"
}
