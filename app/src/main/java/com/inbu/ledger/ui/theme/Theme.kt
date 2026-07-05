package com.inbu.ledger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InbuColorScheme = lightColorScheme(
    primary = Color(0xFF2D5D50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB0E7D4),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4C635A),
    background = Color(0xFFF7F5EF),
    surface = Color(0xFFF7F5EF),
    onSurface = Color(0xFF1D1C19),
    onSurfaceVariant = Color(0xFF62605A),
)

@Composable
fun InbuLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InbuColorScheme,
        content = content,
    )
}

