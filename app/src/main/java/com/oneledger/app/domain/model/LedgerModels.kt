package com.oneledger.app.domain.model

object TransactionType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
    const val TRANSFER = "TRANSFER"
}

object AccountType {
    const val CASH = "CASH"
    const val BANK = "BANK"
    const val CREDIT = "CREDIT"
    const val DIGITAL = "DIGITAL"
    const val INVESTMENT = "INVESTMENT"
}

object SavingsMethod {
    const val FIXED = "FIXED"
    const val FLEXIBLE = "FLEXIBLE"
    const val WEEK_52 = "WEEK_52"
    const val DAY_365 = "DAY_365"
}

data class NewTransaction(
    val type: String,
    val amountMinor: Long,
    val categoryId: String?,
    val accountId: String,
    val toAccountId: String? = null,
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
)
