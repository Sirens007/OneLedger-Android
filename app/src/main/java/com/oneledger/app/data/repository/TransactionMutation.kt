package com.oneledger.app.data.repository

import com.oneledger.app.data.local.TransactionEntity
import com.oneledger.app.domain.model.NewTransaction
import com.oneledger.app.domain.model.TransactionType

internal fun NewTransaction.requireValid() {
    require(type in setOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)) {
        "账单类型无效"
    }
    require(amountMinor > 0) { "账单金额必须大于零" }
    require(accountId.isNotBlank()) { "必须选择账户" }
    when (type) {
        TransactionType.TRANSFER -> {
            require(!toAccountId.isNullOrBlank()) { "转账必须选择转入账户" }
            require(toAccountId != accountId) { "转出和转入账户不能相同" }
        }
        else -> require(!categoryId.isNullOrBlank()) { "必须选择分类" }
    }
}

internal fun TransactionEntity.withUpdate(
    transaction: NewTransaction,
    updatedAt: Long,
): TransactionEntity {
    transaction.requireValid()
    val isTransfer = transaction.type == TransactionType.TRANSFER
    return copy(
        type = transaction.type,
        amountMinor = transaction.amountMinor,
        categoryId = if (isTransfer) null else transaction.categoryId,
        accountId = transaction.accountId,
        toAccountId = if (isTransfer) transaction.toAccountId else null,
        note = transaction.note.trim(),
        occurredAt = transaction.occurredAt,
        updatedAt = updatedAt,
        deletedAt = null,
    )
}
