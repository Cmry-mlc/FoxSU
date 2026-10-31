package me.weishu.kernelsu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowInsetsControllerCompat
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.webui.MonetColorsProvider
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.TextStyles
import top.yukonga.miuix.kmp.theme.defaultTextStyles
import me.weishu.kernelsu.ui.theme.nocturneDarkColors
import me.weishu.kernelsu.ui.theme.nocturneLightColors

@Composable
fun MiuixKernelSUTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && systemDarkTheme)
    // FoxSu Nocturne 换肤：Miuix 界面整体使用 Nocturne 设计稿色板
    // （OLED 暗夜 + violet/mint 强调；亮色为 Daylight 变体），几何与动效不变。
    // 直接以 Colors 直供 MiuixTheme，保证设计稿 token 不被动态取色再生成。
    val nocturneColors = if (darkTheme) nocturneDarkColors() else nocturneLightColors()

    // 「定制 → 字体」此前对这套皮肤完全无效：MiuixTheme 只换了色板，textStyles 一直是
    // defaultTextStyles() 的平台默认字。这里把选中的字族铺到全部文字层级上。
    val fontStyle = CustomizationStore.current.fontStyle
    val fontPath = CustomizationStore.current.fontPath
    val textStyles = remember(fontStyle, fontPath) {
        defaultTextStyles().withFontFamily(resolveFontFamily(fontStyle, fontPath))
    }

    MiuixTheme(
        colors = nocturneColors,
        textStyles = textStyles,
        content = {
            LaunchedEffect(darkTheme) {
                val window = (context as? Activity)?.window ?: return@LaunchedEffect
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            MonetColorsProvider.UpdateCss()
            CompositionLocalProvider(
                LocalContentColor provides MiuixTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    )
}

/** Miuix 的 14 档文字全量换字族；fontFamily 为 null（系统默认）时原样返回。 */
private fun TextStyles.withFontFamily(family: FontFamily?): TextStyles {
    if (family == null) return this
    fun androidx.compose.ui.text.TextStyle.f() = copy(fontFamily = family)
    return copy(
        main = main.f(),
        paragraph = paragraph.f(),
        body1 = body1.f(),
        body2 = body2.f(),
        button = button.f(),
        footnote1 = footnote1.f(),
        footnote2 = footnote2.f(),
        headline1 = headline1.f(),
        headline2 = headline2.f(),
        subtitle = subtitle.f(),
        title1 = title1.f(),
        title2 = title2.f(),
        title3 = title3.f(),
        title4 = title4.f(),
    )
}
