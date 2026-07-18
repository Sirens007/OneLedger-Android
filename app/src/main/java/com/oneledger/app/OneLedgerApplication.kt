package com.oneledger.app

import android.app.Application
import com.oneledger.app.data.local.OneLedgerDatabase
import com.oneledger.app.data.repository.LedgerRepository
import com.oneledger.app.data.repository.OfflineLedgerRepository

class OneLedgerApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val database = OneLedgerDatabase.getInstance(application)
    val repository: LedgerRepository = OfflineLedgerRepository(database.ledgerDao())
}
