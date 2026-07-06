package com.inbu.ledger

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.inbu.ledger.ui.InbuLedgerApp
import com.inbu.ledger.ui.theme.InbuLedgerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(Locale.KOREA)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Locale.setDefault(Locale.KOREA)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InbuLedgerTheme {
                InbuLedgerApp()
            }
        }
    }
}
