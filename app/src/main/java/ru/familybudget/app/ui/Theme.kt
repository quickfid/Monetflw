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
    fun header(): Color = if (isSystemInDarkTheme()) Color(0xFF1C2B3A) else Color(0xFFF3EBDD)

    @Composable
    fun border(): Color = if (isSystemInDarkTheme()) Color(0xFF2F465C) else Color(0xFFC9B98A)

    @Composable
    fun tone(t: Tone): Color = if (isSystemInDarkTheme()) {
        when (t) {
            Tone.NEUTRAL -> Color(0xFF152230)
            Tone.INCOME -> Color(0xFF14392F)
            Tone.EXPENSE -> Color(0xFF4A2530)
            Tone.WARN -> Color(0xFF3B3520)
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
    fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF6EE0A8) else Color(0xFF2E6B3A)

    @Composable
    fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFFF8F8F) else Color(0xFFB3261E)

    @Composable
    fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFFFD27A) else Color(0xFFB26A00)

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
            primary = Color(0xFF5CD6C8),
            onPrimary = Color(0xFF00332E),
            primaryContainer = Color(0xFF0F4F4A),
            onPrimaryContainer = Color(0xFFBFF5EE),
            secondaryContainer = Color(0xFF243B4D),
            onSecondaryContainer = Color(0xFFD3E8F5),
            background = Color(0xFF0E1620),
            onBackground = Color(0xFFE7EEF4),
            surface = Color(0xFF152230),
            onSurface = Color(0xFFE7EEF4),
            surfaceVariant = Color(0xFF1C2B3A),
            onSurfaceVariant = Color(0xFFA9BBCB),
            surfaceContainer = Color(0xFF172431),
            outline = Color(0xFF5B7389),
            outlineVariant = Color(0xFF2A3D4F)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1B7F79),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD6F0EC),
            onPrimaryContainer = Color(0xFF00201E),
            secondaryContainer = Color(0xFFDDEFEC),
            onSecondaryContainer = Color(0xFF0A2623),
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
