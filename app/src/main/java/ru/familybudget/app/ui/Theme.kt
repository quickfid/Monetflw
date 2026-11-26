package ru.familybudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class Tone { NEUTRAL, INCOME, EXPENSE, WARN }

object Sheet {
    @Composable fun header(): Color = if (isSystemInDarkTheme()) Color(0xFF243B3A) else Color(0xFFEAF2EC)
    @Composable fun border(): Color = if (isSystemInDarkTheme()) Color(0xFF3C5954) else Color(0xFFD5E2D8)
    @Composable fun tone(t: Tone): Color = if (isSystemInDarkTheme()) {
        when (t) {
            Tone.NEUTRAL -> Color(0xFF202E32)
            Tone.INCOME -> Color(0xFF203D35)
            Tone.EXPENSE -> Color(0xFF493239)
            Tone.WARN -> Color(0xFF443D2B)
        }
    } else {
        when (t) {
            Tone.NEUTRAL -> Color(0xFFFFFFFF)
            Tone.INCOME -> Color(0xFFE3F1E8)
            Tone.EXPENSE -> Color(0xFFF8E8E6)
            Tone.WARN -> Color(0xFFFFF2D9)
        }
    }
}

object AppColors {
    @Composable fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF8AD6AE) else Color(0xFF397B59)
    @Composable fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFFFA6A0) else Color(0xFFB65D59)
    @Composable fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFFFD58A) else Color(0xFF98702F)
    val chart: List<Color> = listOf(
        Color(0xFF5B9B8E), Color(0xFFE18C83), Color(0xFFE5B968), Color(0xFF7899C2),
        Color(0xFF9B8BB8), Color(0xFF75A98A), Color(0xFFD18DAD), Color(0xFF8AA6A0),
        Color(0xFFC28D69), Color(0xFF7196A5), Color(0xFFB4A06A), Color(0xFF9B9BAF)
    )
}

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF8BCFC0), onPrimary = Color(0xFF17352F),
            primaryContainer = Color(0xFF315B52), onPrimaryContainer = Color(0xFFD8F3EA),
            secondary = Color(0xFFB7C9E0), secondaryContainer = Color(0xFF344653),
            onSecondaryContainer = Color(0xFFE0EAF3),
            background = Color(0xFF151F22), onBackground = Color(0xFFE7EFEB),
            surface = Color(0xFF202C30), onSurface = Color(0xFFE7EFEB),
            surfaceVariant = Color(0xFF2B393C), onSurfaceVariant = Color(0xFFB7C6C0),
            surfaceContainer = Color(0xFF243236), outline = Color(0xFF627A73),
            outlineVariant = Color(0xFF3B4B4A)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF4F8879), onPrimary = Color.White,
            primaryContainer = Color(0xFFDCEDE5), onPrimaryContainer = Color(0xFF173B32),
            secondary = Color(0xFF647F9E), secondaryContainer = Color(0xFFE5ECF4),
            onSecondaryContainer = Color(0xFF26394E),
            background = Color(0xFFF6F8F5), onBackground = Color(0xFF25332F),
            surface = Color(0xFFFFFEFC), onSurface = Color(0xFF25332F),
            surfaceVariant = Color(0xFFEDF2EE), onSurfaceVariant = Color(0xFF687771),
            surfaceContainer = Color(0xFFEEF3EF), outline = Color(0xFFB8C9C0),
            outlineVariant = Color(0xFFDCE5DF)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
