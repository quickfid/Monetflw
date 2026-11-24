package ru.familybudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    @Composable
    fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF5FD3A0) else Color(0xFF167D55)

    @Composable
    fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFFF8A80) else Color(0xFFC62828)

    @Composable
    fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFFFCC80) else Color(0xFFB26A00)

    val chart: List<Color> = listOf(
        Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853),
        Color(0xFFFF6D01), Color(0xFF46BDC6), Color(0xFF7BAAF7), Color(0xFFF07B72),
        Color(0xFF9C27B0), Color(0xFF57BB8A), Color(0xFFFFA165), Color(0xFF795548),
        Color(0xFF607D8B), Color(0xFFE91E63), Color(0xFFCDDC39)
    )
}

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF5FD3A0),
            onPrimary = Color(0xFF00382A),
            primaryContainer = Color(0xFF0E5C3E),
            onPrimaryContainer = Color(0xFFB8F2D8),
            background = Color(0xFF101412),
            surface = Color(0xFF101412),
            surfaceVariant = Color(0xFF1E2622)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF167D55),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCDEBDD),
            onPrimaryContainer = Color(0xFF00210F),
            background = Color(0xFFF5F7F6),
            surface = Color(0xFFF5F7F6),
            surfaceVariant = Color(0xFFE6EDE9)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
