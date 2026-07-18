package com.oneledger.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LedgerBookEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsPlanEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OneLedgerDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile
        private var instance: OneLedgerDatabase? = null

        fun getInstance(context: Context): OneLedgerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OneLedgerDatabase::class.java,
                    "oneledger.db",
                ).build().also { instance = it }
            }
    }
}
