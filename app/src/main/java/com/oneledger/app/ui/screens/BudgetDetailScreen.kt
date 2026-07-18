package com.oneledger.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.app.data.local.BudgetEntity
import com.oneledger.app.data.local.CategoryEntity
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.OneLedgerUiState
import com.oneledger.app.ui.components.CategoryGlyph
import com.oneledger.app.ui.components.DetailHeader
import com.oneledger.app.ui.components.OneLedgerCard
import com.oneledger.app.ui.components.PressableSurface
import com.oneledger.app.ui.components.SectionTitle
import com.oneledger.app.ui.components.colorFromLong
import com.oneledger.app.ui.theme.BrandBlue
import com.oneledger.app.ui.theme.BrandTeal
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.MonthWindow
import com.oneledger.app.util.isIn
import com.oneledger.app.util.monthLabel

private data class BudgetEditorTarget(
    val categoryId: String?,
    val title: String,
    val currentMinor: Long,
)

@Composable
fun BudgetDetailScreen(
    state: OneLedgerUiState,
    onBack: () -> Unit,
    onSaveBudget: (MonthWindow, String?, Long) -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val monthWindow = remember(monthOffset, nowMillis) { MonthWindow.offset(monthOffset, nowMillis) }
    val monthBudgets = state.budgets.filter { it.periodStart == monthWindow.start }
    val totalBudget = monthBudgets.firstOrNull { it.categoryId == null }
    var editorTarget by remember { mutableStateOf<BudgetEditorTarget?>(null) }
    val categoryBudgets = monthBudgets.filter { it.categoryId != null }.associateBy { it.categoryId }
    val expenseTransactions = state.transactions.filter {
        it.type == TransactionType.EXPENSE && it.occurredAt.isIn(monthWindow)
    }
    val totalSpent = expenseTransactions.sumOf { it.amountMinor }
    val spentByCategory = expenseTransactions.groupBy { it.categoryId }.mapValues { entry ->
        entry.value.sumOf { it.amountMinor }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        DetailHeader(
            title = "预算轨道",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthNavigator(
                    label = monthWindow.monthLabel(),
                    canMoveForward = monthOffset < 0,
                    onPrevious = { monthOffset -= 1 },
                    onNext = { monthOffset += 1 },
                )
            }
            item {
                TotalBudgetCard(
                    budget = totalBudget,
                    spentMinor = totalSpent,
                    onEdit = {
                        editorTarget = BudgetEditorTarget(
                            categoryId = null,
                            title = "总预算",
                            currentMinor = totalBudget?.limitMinor ?: 0,
                        )
                    },
                )
            }
            item {
                SectionTitle(
                    title = "分类刻度",
                    trailing = "已设置 ${categoryBudgets.size}/${state.expenseCategories.size}",
                )
            }
            if (state.expenseCategories.isEmpty()) {
                item {
                    OneLedgerCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = "还没有支出分类",
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.expenseCategories, key = { it.id }) { category ->
                    CategoryBudgetRow(
                        category = category,
                        budget = categoryBudgets[category.id],
                        spentMinor = spentByCategory[category.id] ?: 0,
                        onClick = {
                            editorTarget = BudgetEditorTarget(
                                categoryId = category.id,
                                title = "${category.name}预算",
                                currentMinor = categoryBudgets[category.id]?.limitMinor ?: 0,
                            )
                        },
                    )
                }
            }
        }
    }

    editorTarget?.let { target ->
        BudgetEditorSheet(
            target = target,
            onDismiss = { editorTarget = null },
            onSave = { amountMinor ->
                onSaveBudget(monthWindow, target.categoryId, amountMinor)
                editorTarget = null
            },
        )
    }
}

@Composable
private fun MonthNavigator(
    label: String,
    canMoveForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    OneLedgerCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodButton(Icons.Default.ChevronLeft, "上个月", true, onPrevious)
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            PeriodButton(Icons.Default.ChevronRight, "下个月", canMoveForward, onNext)
        }
    }
}

@Composable
private fun PeriodButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(11.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.28f),
            )
        }
    }
}

@Composable
private fun TotalBudgetCard(
    budget: BudgetEntity?,
    spentMinor: Long,
    onEdit: () -> Unit,
) {
    val limit = budget?.limitMinor ?: 0
    val remaining = if (limit > 0) limit - spentMinor else 0
    val progress = if (limit <= 0) 0f else (spentMinor.toFloat() / limit).coerceIn(0f, 1f)
    val statusColor = if (remaining >= 0) BrandTeal else ExpenseCoral

    OneLedgerCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(BrandBlue, CircleShape),
                )
                Text(
                    text = "支出总预算",
                    modifier = Modifier.padding(start = 9.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text("MONTHLY", color = BrandTeal, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MoneyFormatter.format(limit),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
                )
                PressableSurface(
                    onClick = onEdit,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(34.dp),
                    color = BrandBlue.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(11.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = "设置总预算", tint = BrandBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                BudgetMetric("已用", MoneyFormatter.format(spentMinor), Modifier.weight(1f))
                BudgetMetric(
                    when {
                        limit <= 0 -> "待设定"
                        remaining >= 0 -> "可用"
                        else -> "超支"
                    },
                    MoneyFormatter.format(kotlin.math.abs(remaining)),
                    Modifier.weight(1f),
                    statusColor,
                )
            }
        }
    }
}

@Composable
private fun BudgetMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
    }
}

@Composable
private fun CategoryBudgetRow(
    category: CategoryEntity,
    budget: BudgetEntity?,
    spentMinor: Long,
    onClick: () -> Unit,
) {
    val limit = budget?.limitMinor ?: 0
    val progress = if (limit <= 0) 0f else (spentMinor.toFloat() / limit).coerceIn(0f, 1f)
    val overBudget = limit > 0 && spentMinor > limit
    val accent = if (overBudget) ExpenseCoral else colorFromLong(category.colorHex)

    PressableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryGlyph(category.iconKey, colorFromLong(category.colorHex), contentDescription = category.name)
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (limit > 0) MoneyFormatter.format(limit) else "设置预算",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (limit > 0) MaterialTheme.colorScheme.onSurface else BrandBlue,
                    )
                }
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (limit > 0) {
                        "已用 ${MoneyFormatter.format(spentMinor)} · 剩余 ${MoneyFormatter.format((limit - spentMinor).coerceAtLeast(0))}"
                    } else {
                        "本月支出 ${MoneyFormatter.format(spentMinor)} · 尚未设定刻度"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overBudget) ExpenseCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorSheet(
    target: BudgetEditorTarget,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 42.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), CircleShape),
            )
        },
    ) {
        BudgetEditorContent(
            title = target.title,
            currentMinor = target.currentMinor,
            requestFocus = true,
            onSave = onSave,
        )
    }
}

@Composable
internal fun BudgetEditorPreviewSurface() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 18.dp,
        ) {
            Column {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 8.dp)
                        .size(width = 42.dp, height = 4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), CircleShape),
                )
                BudgetEditorContent(
                    title = "总预算",
                    currentMinor = 500_000,
                    requestFocus = false,
                    onSave = {},
                )
            }
        }
    }
}

@Composable
private fun BudgetEditorContent(
    title: String,
    currentMinor: Long,
    requestFocus: Boolean,
    onSave: (Long) -> Unit,
) {
    var amount by rememberSaveable(title, currentMinor) { mutableStateOf(currentMinor.toEditableAmount()) }
    val amountMinor = MoneyFormatter.parseToMinor(amount)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun commit() {
        amountMinor?.let {
            focusManager.clearFocus()
            onSave(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("设置$title", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "金额按月保存，仅存储在本机",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(BrandTeal.copy(alpha = 0.16f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                if (input.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amount = input
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text("预算金额") },
            prefix = { Text("¥") },
            placeholder = { Text("0.00") },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBlue,
                focusedLabelColor = BrandBlue,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
        )
        Spacer(Modifier.height(14.dp))
        Text("快捷刻度", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(50_000L, 100_000L, 200_000L, 500_000L).forEach { preset ->
                PressableSurface(
                    onClick = { amount = preset.toEditableAmount() },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "¥${preset / 100}",
                        modifier = Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { commit() },
            enabled = amountMinor != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("保存预算", modifier = Modifier.padding(start = 7.dp), style = MaterialTheme.typography.titleMedium)
        }
    }

    LaunchedEffect(requestFocus) {
        if (requestFocus) focusRequester.requestFocus()
    }
}

private fun Long.toEditableAmount(): String = when {
    this == 0L -> ""
    this % 100L == 0L -> (this / 100L).toString()
    else -> "%.2f".format(java.util.Locale.US, this / 100.0)
}
