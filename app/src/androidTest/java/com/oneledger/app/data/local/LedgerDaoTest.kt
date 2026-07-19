package com.oneledger.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oneledger.app.domain.model.AccountType
import com.oneledger.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerDaoTest {
    private lateinit var database: OneLedgerDatabase
    private lateinit var dao: LedgerDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OneLedgerDatabase::class.java).build()
        dao = database.ledgerDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun queriesAndBalancesStayInsideSelectedBook() = runBlocking {
        dao.upsertBook(book("book-a", "A"))
        dao.upsertBook(book("book-b", "B"))
        dao.upsertAccounts(
            listOf(
                account("account-a", "book-a", 10_000),
                account("account-b", "book-b", 20_000),
            ),
        )
        dao.upsertCategories(
            listOf(
                category("category-a", "book-a"),
                category("category-b", "book-b"),
            ),
        )
        dao.insertTransaction(transaction("transaction-a", "book-a", "account-a", "category-a", 1_200))
        dao.insertTransaction(transaction("transaction-b", "book-b", "account-b", "category-b", 3_400))

        val bookATransactions = dao.observeTransactions("book-a").first()
        val bookABalances = dao.observeAccountBalances("book-a").first()

        assertEquals(listOf("transaction-a"), bookATransactions.map { it.id })
        assertEquals(listOf(AccountBalanceItem("account-a", 8_800)), bookABalances)
        assertTrue(dao.accountExists("book-a", "account-a"))
        assertTrue(!dao.accountExists("book-a", "account-b"))
    }

    @Test
    fun upsertingBookDoesNotDeleteItsChildren() = runBlocking {
        dao.upsertBook(book("book-a", "旧名称"))
        dao.upsertAccounts(listOf(account("account-a", "book-a", 10_000)))

        dao.upsertBook(book("book-a", "新名称"))

        assertEquals(listOf("account-a"), dao.observeAccounts("book-a").first().map { it.id })
    }

    private fun book(id: String, name: String) = LedgerBookEntity(
        id = id,
        name = name,
        currencyCode = "CNY",
        isDefault = false,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun account(id: String, bookId: String, openingBalance: Long) = AccountEntity(
        id = id,
        bookId = bookId,
        name = id,
        subtitle = "",
        type = AccountType.CASH,
        colorHex = 1,
        openingBalanceMinor = openingBalance,
        includeInNetWorth = true,
        sortOrder = 0,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun category(id: String, bookId: String) = CategoryEntity(
        id = id,
        bookId = bookId,
        name = id,
        transactionType = TransactionType.EXPENSE,
        iconKey = "other",
        colorHex = 1,
        sortOrder = 0,
        isSystem = false,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun transaction(
        id: String,
        bookId: String,
        accountId: String,
        categoryId: String,
        amountMinor: Long,
    ) = TransactionEntity(
        id = id,
        bookId = bookId,
        type = TransactionType.EXPENSE,
        amountMinor = amountMinor,
        categoryId = categoryId,
        accountId = accountId,
        occurredAt = 1,
        createdAt = 1,
        updatedAt = 1,
    )
}
