package com.oneledger.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions_new` (
                `id` TEXT NOT NULL,
                `bookId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL,
                `categoryId` TEXT,
                `accountId` TEXT NOT NULL,
                `toAccountId` TEXT,
                `merchant` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `occurredAt` INTEGER NOT NULL,
                `relatedTransactionId` TEXT,
                `reimbursementState` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deletedAt` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`bookId`) REFERENCES `ledger_books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `transactions_new` (
                `id`, `bookId`, `type`, `amountMinor`, `categoryId`, `accountId`, `toAccountId`,
                `merchant`, `note`, `occurredAt`, `relatedTransactionId`, `reimbursementState`,
                `createdAt`, `updatedAt`, `deletedAt`
            )
            SELECT
                t.`id`, t.`bookId`, t.`type`, t.`amountMinor`,
                CASE WHEN EXISTS(SELECT 1 FROM `categories` c WHERE c.`id` = t.`categoryId`)
                    THEN t.`categoryId` ELSE NULL END,
                t.`accountId`,
                CASE WHEN EXISTS(SELECT 1 FROM `accounts` a WHERE a.`id` = t.`toAccountId`)
                    THEN t.`toAccountId` ELSE NULL END,
                t.`merchant`, t.`note`, t.`occurredAt`, t.`relatedTransactionId`,
                t.`reimbursementState`, t.`createdAt`, t.`updatedAt`, t.`deletedAt`
            FROM `transactions` t
            WHERE EXISTS(SELECT 1 FROM `ledger_books` b WHERE b.`id` = t.`bookId`)
              AND EXISTS(SELECT 1 FROM `accounts` a WHERE a.`id` = t.`accountId`)
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_bookId` ON `transactions` (`bookId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions` (`toAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurredAt` ON `transactions` (`occurredAt`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_relatedTransactionId` " +
                "ON `transactions` (`relatedTransactionId`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets_new` (
                `id` TEXT NOT NULL,
                `bookId` TEXT NOT NULL,
                `categoryId` TEXT,
                `scopeKey` TEXT NOT NULL,
                `periodStart` INTEGER NOT NULL,
                `periodEnd` INTEGER NOT NULL,
                `limitMinor` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`bookId`) REFERENCES `ledger_books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_bookId_scopeKey_periodStart_periodEnd` " +
                "ON `budgets_new` (`bookId`, `scopeKey`, `periodStart`, `periodEnd`)",
        )
        db.execSQL(
            """
            INSERT OR REPLACE INTO `budgets_new` (
                `id`, `bookId`, `categoryId`, `scopeKey`, `periodStart`, `periodEnd`,
                `limitMinor`, `createdAt`, `updatedAt`
            )
            SELECT
                b.`id`, b.`bookId`,
                CASE WHEN EXISTS(SELECT 1 FROM `categories` c WHERE c.`id` = b.`categoryId`)
                    THEN b.`categoryId` ELSE NULL END,
                CASE WHEN EXISTS(SELECT 1 FROM `categories` c WHERE c.`id` = b.`categoryId`)
                    THEN b.`categoryId` ELSE '$TOTAL_BUDGET_SCOPE' END,
                b.`periodStart`, b.`periodEnd`, b.`limitMinor`, b.`createdAt`, b.`updatedAt`
            FROM `budgets` b
            WHERE EXISTS(SELECT 1 FROM `ledger_books` book WHERE book.`id` = b.`bookId`)
            ORDER BY b.`updatedAt`, b.`rowid`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `budgets`")
        db.execSQL("ALTER TABLE `budgets_new` RENAME TO `budgets`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_bookId` ON `budgets` (`bookId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")
    }
}

/**
 * Restores the original empty-budget experience without overwriting a budget the user edited.
 *
 * The old demo seed is identifiable by its default-book id, generated budget id, total-budget
 * scope, 5,000 CNY value, and unchanged created/updated timestamps. User-edited rows receive a new
 * updatedAt value in [OfflineLedgerRepository.saveBudget] and are intentionally left untouched.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE `budgets`
            SET `limitMinor` = 0
            WHERE `bookId` = 'book-default'
              AND `id` LIKE 'budget-%'
              AND `categoryId` IS NULL
              AND `scopeKey` = '$TOTAL_BUDGET_SCOPE'
              AND `limitMinor` = 500000
              AND `createdAt` = `updatedAt`
            """.trimIndent(),
        )
    }
}
