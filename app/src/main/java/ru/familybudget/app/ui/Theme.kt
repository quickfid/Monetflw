package ru.familybudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class Tone { NEUTRAL, INCOME, EXPENSE, WARN, SAVINGS }

object Sheet {
    @Composable fun header(): Color = if (isSystemInDarkTheme()) Color(0x40223631) else Color(0x66FFFFFF)
    /** Настоящее полупрозрачное «жидкое стекло» (Apple Liquid Glass) без тени-артефакта. */
    @Composable fun glass(): Color = if (isSystemInDarkTheme()) Color(0x3D14222A) else Color(0x66FFFFFF)
    @Composable fun border(): Color = if (isSystemInDarkTheme()) Color(0x4DFFFFFF) else Color(0xD9FFFFFF)
    @Composable fun glassBorderBrush(): Brush = if (isSystemInDarkTheme()) {
        Brush.linearGradient(listOf(Color(0x73FFFFFF), Color(0x1AFFFFFF), Color(0x45FFFFFF)))
    } else {
        Brush.linearGradient(listOf(Color(0xFAFFFFFF), Color(0x59FFFFFF), Color(0xCCFFFFFF)))
    }
    @Composable fun tone(t: Tone): Color = if (isSystemInDarkTheme()) {
        when (t) {
            Tone.NEUTRAL -> Color(0x3D14222A)
            Tone.INCOME -> Color(0x4D124735)
            Tone.EXPENSE -> Color(0x4D5C2229)
            Tone.WARN -> Color(0x4D594316)
            Tone.SAVINGS -> Color(0x4D543D14)
        }
    } else {
        when (t) {
            Tone.NEUTRAL -> Color(0x66FFFFFF)
            Tone.INCOME -> Color(0x66B8F2D6)
            Tone.EXPENSE -> Color(0x66FBCACA)
            Tone.WARN -> Color(0x66FEE4A6)
            Tone.SAVINGS -> Color(0x66FCE5AE)
        }
    }
}

object AppColors {
    @Composable fun income(): Color = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF128A54)
    @Composable fun expense(): Color = if (isSystemInDarkTheme()) Color(0xFFF87171) else Color(0xFFD14845)
    @Composable fun warning(): Color = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFC47A10)
    @Composable fun savings(): Color = if (isSystemInDarkTheme()) Color(0xFFF59E0B) else Color(0xFFB87B12)
    @Composable fun accentBlue(): Color = if (isSystemInDarkTheme()) Color(0x471B3B4D) else Color(0x66CBE8FA)

    /** Насыщенный атмосферный градиент фона, сквозь который ярко просвечивают прозрачные стеклянные карточки. */
    @Composable fun ambientBackground(): Brush = if (isSystemInDarkTheme()) {
        Brush.verticalGradient(listOf(Color(0xFF0A2433), Color(0xFF0D2824), Color(0xFF0B1C2B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFAADDFB), Color(0xFFC8EFE9), Color(0xFFBADAFB)))
    }

    @Composable fun heroBrush(): Brush = if (isSystemInDarkTheme()) {
        Brush.linearGradient(listOf(Color(0xA61B4D42), Color(0xA612394A)))
    } else {
        Brush.linearGradient(listOf(Color(0xB81B6B7A), Color(0xB8145975)))
    }

    val chart: List<Color> = listOf(
        Color(0xFF1B6B7A), Color(0xFFDC5B57), Color(0xFFF59E0B), Color(0xFF34D399),
        Color(0xFF7C6EE6), Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFF14B8A6),
        Color(0xFFF97316), Color(0xFF6366F1), Color(0xFF84CC16), Color(0xFF64748B)
    )
}

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        // Премиальный тёмный (изумруд + золото)
        darkColorScheme(
            primary = Color(0xFF34D399),
            onPrimary = Color(0xFF07291D),
            primaryContainer = Color(0xFF16382C),
            onPrimaryContainer = Color(0xFFD1FAE5),
            secondary = Color(0xFF60A5FA),
            onSecondary = Color(0xFF0D213B),
            secondaryContainer = Color(0xFF1A2B3D),
            onSecondaryContainer = Color(0xFFDBEAFE),
            tertiary = Color(0xFFF59E0B),
            onTertiary = Color(0xFF2B1B03),
            tertiaryContainer = Color(0xFF382910),
            onTertiaryContainer = Color(0xFFFEF3C7),
            background = Color(0xFF0F1513),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF18211E),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF212E2A),
            onSurfaceVariant = Color(0xFFCBD5E1),
            surfaceContainer = Color(0xFF151E1B),
            outline = Color(0xFF334740),
            outlineVariant = Color(0xFF263630)
        )
    } else {
        // Современный финтех (чистый светлый + глубокий бирюзово-синий)
        lightColorScheme(
            primary = Color(0xFF1B6B7A),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE2F1F4),
            onPrimaryContainer = Color(0xFF0E3C46),
            secondary = Color(0xFF16A364),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE6F6EE),
            onSecondaryContainer = Color(0xFF0D4A2E),
            tertiary = Color(0xFFC78A1A),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFEF6E4),
            onTertiaryContainer = Color(0xFF523605),
            background = Color(0xFFECF1F7),
            onBackground = Color(0xFF0B141C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0B141C),
            surfaceVariant = Color(0xFFEDF1F7),
            onSurfaceVariant = Color(0xFF283747),
            surfaceContainer = Color(0xFFF1F5F9),
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
