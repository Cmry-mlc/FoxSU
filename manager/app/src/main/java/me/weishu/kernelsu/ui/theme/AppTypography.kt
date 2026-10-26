package me.weishu.kernelsu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/**
 * 全局字体：
 * 0 系统默认 / 1 衬线 / 2 等宽 / 3 手写体 / 4 用户导入的字体文件。
 * 除系统默认外，其余都基于同一套 Typography 只替换 fontFamily，字号行高不变。
 */
fun typographyWithFont(style: Int, fontPath: String? = null): Typography {
    val family = when {
        style == 4 && fontPath != null -> runCatching {
            FontFamily(Font(File(fontPath)))
        }.getOrNull()
        style == 1 -> FontFamily.Serif
        style == 2 -> FontFamily.Monospace
        style == 3 -> FontFamily.Cursive
        else -> null
    } ?: return Typography

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

/** 主题里调用：字体文件只在路径变化时重新加载。 */
@Composable
fun rememberTypography(style: Int, fontPath: String?): Typography =
    remember(style, fontPath) { typographyWithFont(style, fontPath) }
