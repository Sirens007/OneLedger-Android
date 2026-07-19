package com.oneledger.app.ui.screens

import com.oneledger.app.data.local.AccountBalanceItem
import com.oneledger.app.data.local.AccountEntity
import com.oneledger.app.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetsCalculationsTest {
    @Test
    fun excludedAccountsDoNotAffectNetWorth() {
        val accounts = listOf(
            account("cash", includeInNetWorth = true, openingBalance = 10_000),
            account("credit", includeInNetWorth = true, openingBalance = -4_000),
            account("hidden", includeInNetWorth = false, openingBalance = 900_000),
        )
        val summary = calculateNetWorth(
            accounts = accounts,
            balanceItems = listOf(
                AccountBalanceItem("cash", 12_500),
                AccountBalanceItem("credit", -5_500),
                AccountBalanceItem("hidden", 1_000_000),
            ),
        )

        assertEquals(12_500, summary.assetsMinor)
        assertEquals(5_500, summary.liabilitiesMinor)
        assertEquals(7_000, summary.netWorthMinor)
    }

    private fun account(
        id: String,
        includeInNetWorth: Boolean,
        openingBalance: Long,
    ) = AccountEntity(
        id = id,
        bookId = "book-test",
        name = id,
        subtitle = "",
        type = AccountType.CASH,
        colorHex = 1,
        openingBalanceMinor = openingBalance,
        includeInNetWorth = includeInNetWorth,
        sortOrder = 0,
        createdAt = 1,
        updatedAt = 1,
    )
}
