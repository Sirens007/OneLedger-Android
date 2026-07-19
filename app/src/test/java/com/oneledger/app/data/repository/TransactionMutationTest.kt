package com.oneledger.app.data.repository

import com.oneledger.app.data.local.TransactionEntity
import com.oneledger.app.domain.model.NewTransaction
import com.oneledger.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionMutationTest {
    @Test
    fun updatePreservesIdentityAndCreationMetadata() {
        val existing = transactionEntity()
        val updated = existing.withUpdate(
            transaction = NewTransaction(
                type = TransactionType.INCOME,
                amountMinor = 98_765,
                categoryId = "income-salary",
                accountId = "account-daily",
                note = "  七月工资  ",
                occurredAt = 20_000,
            ),
            updatedAt = 30_000,
        )

        assertEquals(existing.id, updated.id)
        assertEquals(existing.bookId, updated.bookId)
        assertEquals(existing.createdAt, updated.createdAt)
        assertEquals(TransactionType.INCOME, updated.type)
        assertEquals(98_765, updated.amountMinor)
        assertEquals("七月工资", updated.note)
        assertEquals(30_000, updated.updatedAt)
        assertNull(updated.deletedAt)
    }

    @Test
    fun transferClearsCategoryAndKeepsBothAccounts() {
        val updated = transactionEntity().withUpdate(
            transaction = NewTransaction(
                type = TransactionType.TRANSFER,
                amountMinor = 12_300,
                categoryId = "expense-food",
                accountId = "account-cash",
                toAccountId = "account-daily",
                occurredAt = 20_000,
            ),
            updatedAt = 30_000,
        )

        assertNull(updated.categoryId)
        assertEquals("account-cash", updated.accountId)
        assertEquals("account-daily", updated.toAccountId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun transferRejectsSameSourceAndDestination() {
        NewTransaction(
            type = TransactionType.TRANSFER,
            amountMinor = 100,
            categoryId = null,
            accountId = "account-cash",
            toAccountId = "account-cash",
        ).requireValid()
    }

    private fun transactionEntity() = TransactionEntity(
        id = "transaction-1",
        bookId = "book-default",
        type = TransactionType.EXPENSE,
        amountMinor = 1_500,
        categoryId = "expense-food",
        accountId = "account-cash",
        note = "午餐",
        occurredAt = 10_000,
        createdAt = 9_000,
        updatedAt = 9_000,
        deletedAt = 15_000,
    )
}
