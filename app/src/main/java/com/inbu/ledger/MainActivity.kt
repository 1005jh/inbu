package com.inbu.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.inbu.ledger.ui.InbuLedgerApp
import com.inbu.ledger.ui.theme.InbuLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InbuLedgerTheme {
                InbuLedgerApp()
            }
        }
    }
}

