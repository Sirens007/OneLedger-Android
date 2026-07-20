package com.oneledger.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OneLedgerDatabase::class.java,
    )

    @Test
    fun migrate1To2PreservesDataAndAddsConstraints() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO ledger_books
                    (id, name, currencyCode, isDefault, createdAt, updatedAt)
                VALUES ('book-test', '迁移账本', 'CNY', 1, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO accounts
                    (id, bookId, name, subtitle, type, colorHex, openingBalanceMinor,
                     includeInNetWorth, sortOrder, createdAt, updatedAt, deletedAt)
                VALUES ('account-test', 'book-test', '现金', '', 'CASH', 1, 10000, 1, 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO categories
                    (id, bookId, name, transactionType, iconKey, colorHex, parentId,
                     sortOrder, isSystem, createdAt, updatedAt, deletedAt)
                VALUES ('category-test', 'book-test', '餐饮', 'EXPENSE', 'food', 1, NULL, 0, 1, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO transactions
                    (id, bookId, type, amountMinor, categoryId, accountId, toAccountId,
                     merchant, note, occurredAt, relatedTransactionId, reimbursementState,
                     createdAt, updatedAt, deletedAt)
                VALUES
                    ('transaction-test', 'book-test', 'EXPENSE', 1250, 'category-test',
                     'account-test', NULL, '', '午餐', 1, NULL, 'NONE', 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO budgets
                    (id, bookId, categoryId, periodStart, periodEnd, limitMinor, createdAt, updatedAt)
                VALUES ('budget-test', 'book-test', NULL, 1, 100, 50000, 1, 1)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, MIGRATION_1_2).apply {
            query("SELECT amountMinor, categoryId FROM transactions WHERE id = 'transaction-test'").use { cursor ->
                assertFalse(cursor.isClosed)
                cursor.moveToFirst()
                assertEquals(1250L, cursor.getLong(0))
                assertEquals("category-test", cursor.getString(1))
            }
            query("SELECT scopeKey, limitMinor FROM budgets WHERE id = 'budget-test'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(TOTAL_BUDGET_SCOPE, cursor.getString(0))
                assertEquals(50000L, cursor.getLong(1))
            }
            query("PRAGMA foreign_key_check").use { cursor ->
                assertEquals(0, cursor.count)
            }
            close()
        }
    }

    @Test
    fun migrate2To3ClearsOnlyUntouchedDemoBudget() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                """
                INSERT INTO ledger_books
                    (id, name, currencyCode, isDefault, createdAt, updatedAt)
                VALUES ('book-default', 'Default', 'CNY', 1, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO budgets
                    (id, bookId, categoryId, scopeKey, periodStart, periodEnd,
                     limitMinor, createdAt, updatedAt)
                VALUES
                    ('budget-untouched', 'book-default', NULL, '$TOTAL_BUDGET_SCOPE',
                     1, 100, 500000, 10, 10),
                    ('budget-user-edited', 'book-default', NULL, '$TOTAL_BUDGET_SCOPE',
                     101, 200, 500000, 10, 20)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, MIGRATION_2_3).apply {
            query("SELECT id, limitMinor FROM budgets ORDER BY id").use { cursor ->
                cursor.moveToFirst()
                assertEquals("budget-untouched", cursor.getString(0))
                assertEquals(0L, cursor.getLong(1))
                cursor.moveToNext()
                assertEquals("budget-user-edited", cursor.getString(0))
                assertEquals(500000L, cursor.getLong(1))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
