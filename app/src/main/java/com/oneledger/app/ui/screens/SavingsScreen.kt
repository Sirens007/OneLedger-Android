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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneledger.app.data.local.SavingsPlanEntity
import com.oneledger.app.domain.model.SavingsMethod
import com.oneledger.app.ui.OneLedgerUiState
import com.oneledger.app.ui.components.OneLedgerCard
import com.oneledger.app.ui.components.PressableSurface
import com.oneledger.app.ui.components.ScreenHeader
import com.oneledger.app.ui.components.SectionTitle
import com.oneledger.app.ui.components.colorFromLong
import com.oneledger.app.ui.theme.BrandBlue
import com.oneledger.app.ui.theme.IncomeMint
import com.oneledger.app.ui.theme.SavingsAmber
import com.oneledger.app.ui.theme.Violet
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.shortDate

@Composable
fun SavingsScreen(
    state: OneLedgerUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ScreenHeader(
            title = "存钱",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            action = {
                PressableSurface(
                    onClick = {},
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(14.dp),
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
                bottom = 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MethodCard("定额存钱", Icons.Default.CalendarMonth, Color(0xFF7E8B9A), Modifier.weight(1f))
                    MethodCard("灵活存钱", Icons.Default.WaterDrop, BrandBlue, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MethodCard("52 周计划", Icons.Default.Timeline, Violet, Modifier.weight(1f))
                    MethodCard("365 天计划", Icons.Default.Savings, IncomeMint, Modifier.weight(1f))
                }
            }
        }

        item { SectionTitle("目标轨道", trailing = "${state.savingsPlans.size} 项") }

        item {
            OneLedgerCard(Modifier.fillMaxWidth()) {
                if (state.savingsPlans.isEmpty()) {
                    Text(
                        "创建一个目标，让每次存下的钱都有去处。",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column {
                        state.savingsPlans.forEachIndexed { index, plan ->
                            SavingsPlanRow(plan)
                            if (index != state.savingsPlans.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MethodCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
) {
    PressableSurface(onClick = {}, modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Text(
                title,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SavingsPlanRow(plan: SavingsPlanEntity) {
    val progress = if (plan.targetMinor <= 0) 0f else (plan.savedMinor.toFloat() / plan.targetMinor).coerceIn(0f, 1f)
    val color = colorFromLong(plan.colorHex)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Savings, contentDescription = null, tint = color)
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        plan.method.methodLabel(),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                plan.endAt?.let {
                    Text(
                        "${plan.startAt.shortDate()} - ${it.shortDate()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "查看计划", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f)) {
                Text("已存金额", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormatter.format(plan.savedMinor), style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f)) {
                Text("目标金额", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(MoneyFormatter.format(plan.targetMinor), style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("进度", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .weight(1f)
                    .height(6.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun String.methodLabel(): String = when (this) {
    SavingsMethod.FIXED -> "定额存钱"
    SavingsMethod.FLEXIBLE -> "灵活存钱"
    SavingsMethod.WEEK_52 -> "52 周"
    SavingsMethod.DAY_365 -> "365 天"
    else -> "存钱计划"
}
