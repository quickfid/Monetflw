package ru.familybudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Оттенки плиток, как в таблице: белая, зелёная (доходы), красная (расходы), жёлтая (обязательные). */
enum class Tone { NEUTRAL, INCOME, EXPENSE, WARN }

object Sheet {
    @Composable
    fun header(): Color = if (isSystemInDarkTheme()) Color(0xFF2B2619) else Color(0xFFF3EBDD)

    @Composable
    fun border(): Color = if (isSystemInDarkTheme()) Color(0xFF6B5E3A) else Color(0xFFC9B98A)

    @Composable
    fun tone(t: Tone): Color = if (isSystemInDarkTheme()) {
        when (t) {
            Tone.NEUTRAL -> Color(0xFF1C1912)
            Tone.INCOME -> Color(0xFF2A3626)
            Tone.EXPENSE -> Color(0xFF5E2F2A)
            Tone.WARN -> Color(0xFF4D3C18)
        }
    } else {
        when (t) {
            Tone.NEUTRAL -> Color(0xFFFFFFFF)
            Tone.INCOME -> Color(0xFFE4EBDC)
            Tone.EXPENSE -> Color(0xFFEFA69C)
            Tone.WARN -> Color(0xFFFDE9C5)
        }
    }
}

object AppColors {
    @Composable
    fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF8FD694) else Color(0xFF2E6B3A)

    @Composable
    fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFFF8A80) else Color(0xFFB3261E)

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
            primary = Color(0xFFE6C77E),
            onPrimary = Color(0xFF3B2F00),
            primaryContainer = Color(0xFF54430F),
            onPrimaryContainer = Color(0xFFFDE9C5),
            secondaryContainer = Color(0xFF4A3F1E),
            onSecondaryContainer = Color(0xFFF6E7C1),
            background = Color(0xFF15130E),
            surface = Color(0xFF1C1912),
            surfaceVariant = Color(0xFF2B2619),
            surfaceContainer = Color(0xFF2B2619),
            outline = Color(0xFF8C7C52),
            outlineVariant = Color(0xFF3D3622)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF8A6D1F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFDE9C5),
            onPrimaryContainer = Color(0xFF2A1F00),
            secondaryContainer = Color(0xFFF6E7C1),
            onSecondaryContainer = Color(0xFF2A2000),
            background = Color(0xFFFBF9F5),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF3EBDD),
            surfaceContainer = Color(0xFFF3EBDD),
            outline = Color(0xFFC9B98A),
            outlineVariant = Color(0xFFE3D9BC)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
