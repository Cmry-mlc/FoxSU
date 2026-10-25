package me.weishu.kernelsu.ui.component

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
fun rememberCardSupportingColor(): Color = LocalContentColor.current.copy(alpha = 0.65f)
