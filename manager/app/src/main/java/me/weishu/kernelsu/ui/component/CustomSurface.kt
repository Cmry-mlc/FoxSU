package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.data.settings.CustomizationStore

/** True when the active scheme is the night one (works for forced dark / AMOLED too). */
@Composable
@ReadOnlyComposable
fun isNightScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Card surface with the user card color (or the theme fallback) and opacity applied.
 * Custom colours are a light-mode option: at night the card always uses the dark surface so
 * white cards never glare on a black canvas.
 */
@Composable
@ReadOnlyComposable
fun rememberCardSurface(fallback: Color): Color {
    val custom = CustomizationStore.current
    val useCustom = custom.cardColor != 0 && !isNightScheme()
    val base = if (useCustom) Color(custom.cardColor) else fallback
    return base.copy(alpha = custom.cardAlpha)
}

/** Text color that stays readable on a custom card color. */
@Composable
@ReadOnlyComposable
fun rememberCardContentColor(fallback: Color): Color {
    val custom = CustomizationStore.current
    if (custom.cardColor == 0 || isNightScheme()) return fallback
    return if (Color(custom.cardColor).luminance() > 0.55f) Color(0xFF141A14) else Color.White
}

/** Secondary (supporting) text on a card surface. */
@Composable
@ReadOnlyComposable
fun rememberCardSupportingColor(): Color = LocalContentColor.current.copy(alpha = supportingTextAlpha())

/**
 * Card colour for the ColorUI style. In dark mode the card has to sit *above* the canvas, so we
 * use a higher container tone instead of the lowest one (which is darker than the background and
 * made text look muddy). The surface is translucent so the wallpaper shows through, matching
 * the floating bottom bar.
 */
@Composable
@ReadOnlyComposable
fun freshCardColor(): Color {
    val custom = CustomizationStore.current
    // 玻璃卡面：极淡的白色薄片，靠描边和高光撑出层次（参考图二的透明卡片）
    if (custom.cardGlass) {
        // 深色壁纸上用极淡白片 + 白字；浅色壁纸上用更实的白片保证深色字可读
        val light = glassNeedsLightText()
        return if (light) {
            Color.White.copy(alpha = 0.13f)
        } else {
            Color.White.copy(alpha = if (isNightScheme()) 0.16f else 0.46f)
        }
    }
    val base = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    return base.copy(alpha = 0.84f)
}

/** Hairline that lifts a card off the canvas; only dark mode needs it (shadows read as noise). */
@Composable
@ReadOnlyComposable
fun freshCardBorder(): BorderStroke? {
    val custom = CustomizationStore.current
    if (custom.cardGlass) {
        return BorderStroke(
            1.dp,
            Color.White.copy(alpha = if (glassNeedsLightText()) 0.22f else 0.7f),
        )
    }
    return if (isSystemInDarkTheme()) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    } else {
        null
    }
}

/** Supporting text on a card: a bit brighter in dark mode so it stays legible. */
@Composable
@ReadOnlyComposable
fun supportingTextAlpha(): Float = if (isSystemInDarkTheme()) 0.82f else 0.65f
