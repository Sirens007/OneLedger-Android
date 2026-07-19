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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.data.local.AccountBalanceItem
import com.oneledger.app.domain.model.AccountType
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.OneLedgerUiState
import com.oneledger.app.ui.components.AccountGlyph
import com.oneledger.app.ui.components.LabelValue
import com.oneledger.app.ui.components.OneLedgerCard
import com.oneledger.app.ui.components.PressableSurface
import com.oneledger.app.ui.components.ScreenHeader
import com.oneledger.app.ui.components.SectionTitle
import com.oneledger.app.ui.components.colorFromLong
import com.oneledger.app.ui.theme.BrandTeal
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.ui.theme.IncomeMint
import com.oneledger.app.util.MoneyFormatter
import com.oneledger.app.util.MonthWindow
import com.oneledger.app.util.isIn

@Composable
fun AssetsScreen(
    state: OneLedgerUiState,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val balances = state.accountBalances.associate { it.accountId to it.currentBalanceMinor }
    val netWorthSummary = calculateNetWorth(state.accounts, state.accountBalances)
    val totalAssets = netWorthSummary.assetsMinor
    val totalLiabilities = netWorthSummary.liabilitiesMinor
    val netWorth = netWorthSummary.netWorthMinor
    val currentMonth = remember(nowMillis) { MonthWindow.current(nowMillis) }
    val monthTransactions = state.transactions.filter { it.occurredAt.isIn(currentMonth) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ScreenHeader(
            title = "资产",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            action = {
                PressableSurface(
                    onClick = {},
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "添加账户")
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
            OneLedgerCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(24.dp)
                                .background(BrandTeal, RoundedCornerShape(2.dp)),
                        )
                        Text(
                            "净资产",
                            modifier = Modifier.padding(start = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "隐藏金额",
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(19.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(MoneyFormatter.format(netWorth), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth()) {
                        LabelValue(
                            label = "总资产",
                            value = MoneyFormatter.format(totalAssets),
                            modifier = Modifier.weight(1f),
                            valueColor = IncomeMint,
                        )
                        LabelValue(
                            label = "总负债",
                            value = MoneyFormatter.format(totalLiabilities),
                            modifier = Modifier.weight(1f),
                            valueColor = if (totalLiabilities > 0) ExpenseCoral else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniAssetCard(
                    label = "本月流入",
                    value = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor },
                    color = IncomeMint,
                    modifier = Modifier.weight(1f),
                )
                MiniAssetCard(
                    label = "本月流出",
                    value = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor },
                    color = ExpenseCoral,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle("账户轨道 (${state.accounts.size})", trailing = "净值 ${MoneyFormatter.format(netWorth)}") }

        item {
            OneLedgerCard(Modifier.fillMaxWidth()) {
                if (state.accounts.isEmpty()) {
                    Text(
                        "还没有账户",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column {
                        state.accounts.forEachIndexed { index, account ->
                            AccountRow(account, balances[account.id] ?: account.openingBalanceMinor)
                            if (index != state.accounts.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp),
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

internal data class NetWorthSummary(
    val assetsMinor: Long,
    val liabilitiesMinor: Long,
) {
    val netWorthMinor: Long = assetsMinor - liabilitiesMinor
}

internal fun calculateNetWorth(
    accounts: List<AccountEntity>,
    balanceItems: List<AccountBalanceItem>,
): NetWorthSummary {
    val balanceByAccountId = balanceItems.associate { it.accountId to it.currentBalanceMinor }
    val includedBalances = accounts
        .asSequence()
        .filter { it.includeInNetWorth }
        .map { balanceByAccountId[it.id] ?: it.openingBalanceMinor }
        .toList()
    return NetWorthSummary(
        assetsMinor = includedBalances.filter { it > 0 }.sum(),
        liabilitiesMinor = includedBalances.filter { it < 0 }.sumOf { kotlin.math.abs(it) },
    )
}

@Composable
private fun MiniAssetCard(
    label: String,
    value: Long,
    color: Color,
    modifier: Modifier,
) {
    OneLedgerCard(modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                MoneyFormatter.format(value),
                style = MaterialTheme.typography.titleLarge,
                color = color,
            )
        }
    }
}

@Composable
private fun AccountRow(account: AccountEntity, balance: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountGlyph(account.type, colorFromLong(account.colorHex))
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(account.name, style = MaterialTheme.typography.titleMedium)
            Text(
                account.subtitle.ifBlank { account.type.accountTypeLabel() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            MoneyFormatter.format(balance),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun String.accountTypeLabel(): String = when (this) {
    AccountType.CASH -> "现金钱包"
    AccountType.BANK -> "银行账户"
    AccountType.CREDIT -> "信用账户"
    AccountType.DIGITAL -> "数字钱包"
    else -> "资产账户"
}
