package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.screen.home.HomeActions
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.screen.settings.SettingsScreenActions
import me.weishu.kernelsu.ui.screen.settings.SettingsUiState

/**
 * FoxSu Nocturne —— 设计 token（资料库《nocturne-tokens.json / css》逐值映射）。
 * 暗夜：OLED #07080B + violet #8B7BFF + mint #4DE2C5；亮色 Daylight：#F4F5FA + #6A57F5。
 */
object NcColors {
    // 暗夜
    val bg = Color(0xFF07080B)
    val surface1 = Color(0xFF101218)
    val surface2 = Color(0xFF161922)
    val fill = Color(0x0DFFFFFF)
    val line = Color(0x12FFFFFF)
    val overlay = Color(0x9E040508)
    val violet = Color(0xFF8B7BFF)
    val violetDeep = Color(0xFF6A57F5)
    val violetText = Color(0xFFB0A4FF)
    val violetKicker = Color(0xFF7B6FE8)
    val mint = Color(0xFF4DE2C5)
    val lime = Color(0xFFC9F45C)
    val red = Color(0xFFFF6B5E)
    val redSoft = Color(0xFFFFB3A9)
    val amber = Color(0xFFFFB35C)
    val text1 = Color(0xFFF3F4F8)
    val text2 = Color(0xFF9BA0B2)
    val text3 = Color(0xFF5D6376)
    val textMono = Color(0xFFB9BFCB)
    val iconTint = Color(0xFFA9AFC0)
    val navBg = Color(0xB814161E)

    // 亮色
    val lBg = Color(0xFFF4F5FA)
    val lSurface1 = Color(0xFFFFFFFF)
    val lSurface2 = Color(0xFFF7F8FC)
    val lFill = Color(0xFFF3F4F9)
    val lLine = Color(0x140F142D)
    val lViolet = Color(0xFF6A57F5)
    val lMint = Color(0xFF0FA98B)
    val lRed = Color(0xFFE8503F)
    val lAmber = Color(0xFFDE9A21)
    val lText1 = Color(0xFF12141B)
    val lText2 = Color(0xFF5D6376)
    val lText3 = Color(0xFF9AA0B0)
}

/** 当前是否为 Nocturne 模式（供共享组件判断）。 */
val isNocturneMode: Boolean @Composable get() = LocalUiMode.current == UiMode.Nocturne

/** 圆角（设计稿 radius token）。 */
object NcRadius {
    val sheet = 30.dp
    val hero = 24.dp
    val panel = 20.dp
    val card = 16.dp
    val nav = 22.dp
    val iconBox = 11.dp
    val btn = 13.dp
    val pill = 99.dp
}

/** 间距 / 尺寸（设计稿 spacing & sizing token）。 */
object NcDimens {
    val pageX = 18.dp
    val iconBox = 34.dp
    val appAvatar = 36.dp
    val authorAvatar = 66.dp
    val ring = 56.dp
    val navHeight = 64.dp
    val fab = 56.dp
    val rowPadY = 8.5.dp
    val rowPadX = 15.dp
}

/** 亮暗感知 token 入口：Nocturne 屏幕统一从这里取色。 */
@Composable
fun ncTokens(): NcTokens {
    // Nocturne 是 OLED 暗夜主题：固定使用设计稿暗夜 token，不随系统/应用亮色设置切换
    return NcTokens.dark
}

data class NcTokens(
    val dark: Boolean,
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val fill: Color,
    val line: Color,
    val violet: Color,
    val violetDeep: Color,
    val violetText: Color,
    val violetKicker: Color,
    val mint: Color,
    val lime: Color,
    val red: Color,
    val redSoft: Color,
    val amber: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val textMono: Color,
    val iconTint: Color,
) {
    companion object {
        val dark = NcTokens(
            dark = true,
            bg = NcColors.bg, surface1 = NcColors.surface1, surface2 = NcColors.surface2,
            fill = NcColors.fill, line = NcColors.line,
            violet = NcColors.violet, violetDeep = NcColors.violetDeep, violetText = NcColors.violetText,
            violetKicker = NcColors.violetKicker, mint = NcColors.mint, lime = NcColors.lime,
            red = NcColors.red, redSoft = NcColors.redSoft, amber = NcColors.amber,
            text1 = NcColors.text1, text2 = NcColors.text2, text3 = NcColors.text3,
            textMono = NcColors.textMono, iconTint = NcColors.iconTint,
        )
        val light = NcTokens(
            dark = false,
            bg = NcColors.lBg, surface1 = NcColors.lSurface1, surface2 = NcColors.lSurface2,
            fill = NcColors.lFill, line = NcColors.lLine,
            violet = NcColors.lViolet, violetDeep = NcColors.lViolet, violetText = NcColors.lViolet,
            violetKicker = NcColors.lViolet, mint = NcColors.lMint, lime = NcColors.lime,
            red = NcColors.lRed, redSoft = Color(0xFF8A2318), amber = NcColors.lAmber,
            text1 = NcColors.lText1, text2 = NcColors.lText2, text3 = NcColors.lText3,
            textMono = Color(0xFF4E5464), iconTint = Color(0xFF6F7686),
        )
    }
}
