package com.oneledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneledger.app.ui.OneLedgerApp
import com.oneledger.app.ui.OneLedgerViewModel
import com.oneledger.app.ui.OneLedgerViewModelFactory
import com.oneledger.app.ui.theme.OneLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent { OneLedgerRoot() }
    }
}

@Composable
private fun OneLedgerRoot() {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as OneLedgerApplication
    val viewModel: OneLedgerViewModel = viewModel(
        factory = OneLedgerViewModelFactory(application.container.repository),
    )
    OneLedgerTheme(darkTheme = isSystemInDarkTheme()) {
        OneLedgerApp(viewModel = viewModel)
    }
}
