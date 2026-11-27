package ru.familybudget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LocalGlassAlpha = compositionLocalOf { 0.40f }
val LocalAccentTheme = compositionLocalOf { "teal" }

enum class Tone { NEUTRAL, INCOME, EXPENSE, WARN, SAVINGS }

object Sheet {
    @Composable fun header(): Color {
        val a = LocalGlassAlpha.current
        return if (isSystemInDarkTheme()) Color(0xFF223631).copy(alpha = (a * 0.75f).coerceIn(0.18f, 0.75f))
        else Color.White.copy(alpha = a)
    }
    /** Настоящее полупрозрачное «жидкое стекло» (Apple Liquid Glass) без тени-артефакта. */
    @Composable fun glass(): Color {
        val a = LocalGlassAlpha.current
        return if (isSystemInDarkTheme()) Color(0xFF14222A).copy(alpha = (a * 0.7f).coerceIn(0.16f, 0.75f))
        else Color.White.copy(alpha = a)
    }
    @Composable fun border(): Color = if (isSystemInDarkTheme()) Color(0x4DFFFFFF) else Color(0xD9FFFFFF)
    @Composable fun glassBorderBrush(): Brush = if (isSystemInDarkTheme()) {
        Brush.linearGradient(listOf(Color(0x73FFFFFF), Color(0x1AFFFFFF), Color(0x45FFFFFF)))
    } else {
        Brush.linearGradient(listOf(Color(0xFAFFFFFF), Color(0x59FFFFFF), Color(0xCCFFFFFF)))
    }
    @Composable fun tone(t: Tone): Color {
        val a = LocalGlassAlpha.current
        return if (isSystemInDarkTheme()) {
            val da = (a * 0.78f).coerceIn(0.18f, 0.78f)
            when (t) {
                Tone.NEUTRAL -> Color(0xFF14222A).copy(alpha = da)
                Tone.INCOME -> Color(0xFF124735).copy(alpha = da)
                Tone.EXPENSE -> Color(0xFF5C2229).copy(alpha = da)
                Tone.WARN -> Color(0xFF594316).copy(alpha = da)
                Tone.SAVINGS -> Color(0xFF543D14).copy(alpha = da)
            }
        } else {
            when (t) {
                Tone.NEUTRAL -> Color.White.copy(alpha = a)
                Tone.INCOME -> Color(0xFFB8F2D6).copy(alpha = a)
                Tone.EXPENSE -> Color(0xFFFBCACA).copy(alpha = a)
                Tone.WARN -> Color(0xFFFEE4A6).copy(alpha = a)
                Tone.SAVINGS -> Color(0xFFFCE5AE).copy(alpha = a)
            }
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

    @Composable fun heroBrush(): Brush {
        val accent = LocalAccentTheme.current
        val dark = isSystemInDarkTheme()
        return when (accent) {
            "emerald" -> if (dark) Brush.linearGradient(listOf(Color(0xA60E4D36), Color(0xA60A3828)))
                else Brush.linearGradient(listOf(Color(0xBD127A52), Color(0xBD0D5E41)))
            "indigo" -> if (dark) Brush.linearGradient(listOf(Color(0xA62A2D6B), Color(0xA61D2252)))
                else Brush.linearGradient(listOf(Color(0xBD3E44A8), Color(0xBD2D328A)))
            "graphite" -> if (dark) Brush.linearGradient(listOf(Color(0xB3222A33), Color(0xB3161C24)))
                else Brush.linearGradient(listOf(Color(0xBD2E3A47), Color(0xBD1E2733)))
            else -> if (dark) Brush.linearGradient(listOf(Color(0xA61B4D42), Color(0xA612394A)))
                else Brush.linearGradient(listOf(Color(0xB81B6B7A), Color(0xB8145975)))
        }
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
            onBackground = Color.White,
            surface = Color(0xFF18211E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF212E2A),
            onSurfaceVariant = Color(0xFFE2E8F0),
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
