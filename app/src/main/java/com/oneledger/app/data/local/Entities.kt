package com.oneledger.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ledger_books")
data class LedgerBookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currencyCode: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = LedgerBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val name: String,
    val subtitle: String,
    val type: String,
    val colorHex: Long,
    val openingBalanceMinor: Long,
    val includeInNetWorth: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = LedgerBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index(value = ["bookId", "transactionType", "name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val name: String,
    val transactionType: String,
    val iconKey: String,
    val colorHex: Long,
    val parentId: String? = null,
    val sortOrder: Int,
    val isSystem: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = LedgerBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index("bookId"),
        Index("accountId"),
        Index("toAccountId"),
        Index("categoryId"),
        Index("occurredAt"),
        Index("relatedTransactionId"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val type: String,
    val amountMinor: Long,
    val categoryId: String?,
    val accountId: String,
    val toAccountId: String? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAt: Long,
    val relatedTransactionId: String? = null,
    val reimbursementState: String = "NONE",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = LedgerBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("categoryId")],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val categoryId: String? = null,
    val periodStart: Long,
    val periodEnd: Long,
    val limitMinor: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "savings_plans",
    foreignKeys = [
        ForeignKey(
            entity = LedgerBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class SavingsPlanEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val name: String,
    val method: String,
    val targetMinor: Long,
    val savedMinor: Long,
    val colorHex: Long,
    val startAt: Long,
    val endAt: Long?,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
