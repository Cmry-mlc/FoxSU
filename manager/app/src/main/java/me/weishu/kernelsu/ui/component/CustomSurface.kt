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

/** Card surface with the user card color (or the theme fallback) and opacity applied. */
@Composable
@ReadOnlyComposable
fun rememberCardSurface(fallback: Color): Color {
    val custom = CustomizationStore.current
    val base = if (custom.cardColor == 0) fallback else Color(custom.cardColor)
    return base.copy(alpha = custom.cardAlpha)
}

/** Text color that stays readable on a custom card color. */
@Composable
@ReadOnlyComposable
fun rememberCardContentColor(fallback: Color): Color {
    val custom = CustomizationStore.current
    if (custom.cardColor == 0) return fallback
    return if (Color(custom.cardColor).luminance() > 0.55f) Color(0xFF141A14) else Color.White
}

/** Secondary (supporting) text on a card surface. */
@Composable
@ReadOnlyComposable
fun rememberCardSupportingColor(): Color = LocalContentColor.current.copy(alpha = supportingTextAlpha())

/**
 * Card colour for the ColorUI style. In dark mode the card has to sit *above* the canvas, so we
 * use a higher container tone instead of the lowest one (which is darker than the background and
 * made text look muddy).
 */
@Composable
@ReadOnlyComposable
fun freshCardColor(): Color = if (isSystemInDarkTheme()) {
    MaterialTheme.colorScheme.surfaceContainerHigh
} else {
    MaterialTheme.colorScheme.surfaceContainerLowest
}

/** Hairline that lifts a card off the canvas; only dark mode needs it (shadows read as noise). */
@Composable
@ReadOnlyComposable
fun freshCardBorder(): BorderStroke? = if (isSystemInDarkTheme()) {
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
} else {
    null
}

/** Supporting text on a card: a bit brighter in dark mode so it stays legible. */
@Composable
@ReadOnlyComposable
fun supportingTextAlpha(): Float = if (isSystemInDarkTheme()) 0.82f else 0.65f
