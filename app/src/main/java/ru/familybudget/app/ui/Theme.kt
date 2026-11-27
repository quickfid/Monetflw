package ru.familybudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class Tone { NEUTRAL, INCOME, EXPENSE, WARN, SAVINGS }

object Sheet {
    @Composable fun header(): Color = if (isSystemInDarkTheme()) Color(0xFF1F302A) else Color(0xFFE4EEE6)
    @Composable fun border(): Color = if (isSystemInDarkTheme()) Color(0xFF2F453D) else Color(0xFFD9E3DC)
    @Composable fun tone(t: Tone): Color = if (isSystemInDarkTheme()) {
        when (t) {
            Tone.NEUTRAL -> Color(0xFF1C2522)
            Tone.INCOME -> Color(0xFF1B3429)
            Tone.EXPENSE -> Color(0xFF3E2629)
            Tone.WARN -> Color(0xFF3A3220)
            Tone.SAVINGS -> Color(0xFF342E1E)
        }
    } else {
        when (t) {
            Tone.NEUTRAL -> Color(0xFFFFFFFF)
            Tone.INCOME -> Color(0xFFE4EEE6)
            Tone.EXPENSE -> Color(0xFFF8E9E7)
            Tone.WARN -> Color(0xFFFDF3DC)
            Tone.SAVINGS -> Color(0xFFFAF3E0)
        }
    }
}

object AppColors {
    @Composable fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF7FD1A6) else Color(0xFF39775A)
    @Composable fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFF49D96) else Color(0xFFB85650)
    @Composable fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFEEC67A) else Color(0xFF9C7228)
    @Composable fun savings(): Color = if (isSystemInDarkTheme()) Color(0xFFE2BE6B) else Color(0xFFA67C2E)
    @Composable fun accentBlue(): Color = if (isSystemInDarkTheme()) Color(0xFF27394A) else Color(0xFFDCE8F5)

    val chart: List<Color> = listOf(
        Color(0xFF39775A), Color(0xFFD98278), Color(0xFFC99E48), Color(0xFF6B8FB5),
        Color(0xFF8E7FB0), Color(0xFF6BA688), Color(0xFFC784A3), Color(0xFF7D9A93),
        Color(0xFFBA8460), Color(0xFF678C9C), Color(0xFFA89660), Color(0xFF8E8FA3)
    )
}

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF7FC4A3),
            onPrimary = Color(0xFF102A1F),
            primaryContainer = Color(0xFF234738),
            onPrimaryContainer = Color(0xFFD8F0E3),
            secondary = Color(0xFFA6C2DE),
            onSecondary = Color(0xFF172B3D),
            secondaryContainer = Color(0xFF263848),
            onSecondaryContainer = Color(0xFFDCE8F5),
            tertiary = Color(0xFFE2BE6B),
            onTertiary = Color(0xFF2D230B),
            tertiaryContainer = Color(0xFF3D331B),
            onTertiaryContainer = Color(0xFFF8E7BE),
            background = Color(0xFF121917),
            onBackground = Color(0xFFE5ECE8),
            surface = Color(0xFF1A2320),
            onSurface = Color(0xFFE5ECE8),
            surfaceVariant = Color(0xFF24302C),
            onSurfaceVariant = Color(0xFFA5B3AC),
            surfaceContainer = Color(0xFF1E2925),
            outline = Color(0xFF4F635B),
            outlineVariant = Color(0xFF2D3C36)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF39775A),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE4EEE6),
            onPrimaryContainer = Color(0xFF1D4030),
            secondary = Color(0xFF4F6F8F),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDCE8F5),
            onSecondaryContainer = Color(0xFF20364A),
            tertiary = Color(0xFFA67C2E),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFAF3E0),
            onTertiaryContainer = Color(0xFF47330B),
            background = Color(0xFFF5F7F4),
            onBackground = Color(0xFF34433B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF34433B),
            surfaceVariant = Color(0xFFECEFE9),
            onSurfaceVariant = Color(0xFF78847D),
            surfaceContainer = Color(0xFFF0F4EE),
            outline = Color(0xFFC2CEC6),
            outlineVariant = Color(0xFFDCE4DE)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
