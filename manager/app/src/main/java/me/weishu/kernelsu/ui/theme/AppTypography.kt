package me.weishu.kernelsu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/**
 * 全局字体：0 系统默认 / 1 衬线 / 2 等宽 / 3 手写体。
 * 除系统默认外，其余都基于同一套 Typography 只替换 fontFamily，字号行高不变。
 */
fun typographyWithFont(style: Int): Typography {
    val family = when (style) {
        1 -> FontFamily.Serif
        2 -> FontFamily.Monospace
        3 -> FontFamily.Cursive
        else -> return Typography
    }
    val base = Typography
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
