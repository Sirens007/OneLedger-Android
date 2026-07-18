package com.oneledger.app.data.local

data class TransactionListItem(
    val id: String,
    val type: String,
    val amountMinor: Long,
    val categoryId: String?,
    val categoryName: String,
    val iconKey: String,
    val colorHex: Long,
    val accountId: String,
    val accountName: String,
    val toAccountId: String?,
    val note: String,
    val merchant: String,
    val occurredAt: Long,
)
