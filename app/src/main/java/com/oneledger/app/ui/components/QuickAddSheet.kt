package com.oneledger.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.data.local.CategoryEntity
import com.oneledger.app.domain.model.NewTransaction
import com.oneledger.app.domain.model.TransactionType
import com.oneledger.app.ui.theme.ExpenseCoral
import com.oneledger.app.ui.theme.IncomeMint
import com.oneledger.app.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    accounts: List<AccountEntity>,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (NewTransaction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
    val parsedAmount = MoneyFormatter.parseToMinor(amount)
    val accent = if (type == TransactionType.EXPENSE) ExpenseCoral else IncomeMint

    LaunchedEffect(type, categories) {
        if (categories.none { it.id == selectedCategoryId }) selectedCategoryId = categories.firstOrNull()?.id
    }
    LaunchedEffect(accounts) {
        if (accounts.none { it.id == selectedAccountId }) selectedAccountId = accounts.firstOrNull()?.id
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline, CircleShape),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(Modifier.size(34.dp))
                    Text(
                        "快速入账",
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(3.dp),
                ) {
                    TypeChip("支出", TransactionType.EXPENSE, type, ExpenseCoral) { type = it }
                    TypeChip("收入", TransactionType.INCOME, type, IncomeMint) { type = it }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { value ->
                    if (value.length <= 11 && value.matches(Regex("\\d{0,8}(\\.\\d{0,2})?"))) amount = value
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    textAlign = TextAlign.Center,
                    color = accent,
                    fontSize = 40.sp,
                ),
                prefix = { Text("¥", style = MaterialTheme.typography.headlineLarge, color = accent) },
                placeholder = {
                    Text(
                        "0.00",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedBorderColor = accent.copy(alpha = 0.55f),
                    unfocusedBorderColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(14.dp))
            SheetLabel("分类")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { category ->
                    CategoryChoice(
                        category = category,
                        selected = category.id == selectedCategoryId,
                        onClick = { selectedCategoryId = category.id },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            SheetLabel("账户")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.id }) { account ->
                    AccountChoice(
                        account = account,
                        selected = account.id == selectedAccountId,
                        onClick = { selectedAccountId = account.id },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 60) note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注（可选）") },
                placeholder = { Text("例如：朋友聚餐") },
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            SimpleDateFormat("M月d日", Locale.CHINA).format(Date()),
                            modifier = Modifier.padding(start = 5.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.copy(alpha = 0.55f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )

            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    onSave(
                        NewTransaction(
                            type = type,
                            amountMinor = requireNotNull(parsedAmount),
                            categoryId = selectedCategoryId,
                            accountId = requireNotNull(selectedAccountId),
                            note = note,
                        ),
                    )
                },
                enabled = parsedAmount != null && parsedAmount > 0 && selectedCategoryId != null && selectedAccountId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("写入 OneLedger", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    value: String,
    selected: String,
    color: Color,
    onSelected: (String) -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (value == selected) color else Color.Transparent,
        animationSpec = tween(140),
        label = "transaction-type",
    )
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onSelected(value) }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (value == selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CategoryChoice(category: CategoryEntity, selected: Boolean, onClick: () -> Unit) {
    val color = colorFromLong(category.colorHex)
    Column(
        modifier = Modifier
            .background(
                if (selected) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryGlyph(category.iconKey, color, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(4.dp))
        Text(category.name, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun AccountChoice(account: AccountEntity, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background,
                RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountGlyph(account.type, colorFromLong(account.colorHex), modifier = Modifier.size(36.dp))
        Text(account.name, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
