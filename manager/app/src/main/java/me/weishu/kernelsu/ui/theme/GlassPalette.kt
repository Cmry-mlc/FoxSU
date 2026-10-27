package me.weishu.kernelsu.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Brush.Companion.radialGradient
import me.weishu.kernelsu.ui.component.isNightScheme

/**
 * FoxSu Glass 设计稿 token：
 * 暗色 :root{--bg:#0d0d14 --panel #16161f --panel2 #1c1c28 --line rgba(255,255,255,.07)
 *            --ink #f0eef6 --ink2 #c9c6d6 --sub #8e8b9c --faint #55525f}
 * 亮色（液态玻璃覆盖）：--bg #eef0f7 --panel rgba(255,255,255,.55) --line rgba(255,255,255,.7)
 *            --ink #26262e --ink2 #55525f --sub #7a7788 --faint #a3a0b2
 * 霓虹色暗色版：flame #ff7a59 amber #ffb35c lemon #ffe45c lime #6ef08a
 *               cyan #5ce1e6 blue #7aa2ff violet #c77dff pink #ff7ac8 mint #6ef0c0
 * 霓虹色亮色版：flame #d94f2e amber #c97a10 lemon #a8860e lime #2e9a50
 *               cyan #0e8a99 blue #4d6fd8 violet #8a3fd0 pink #d84f9a mint #1e9a78
 */
data class GlassTokens(
    val isDark: Boolean,
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val sub: Color,
    val faint: Color,
    val flame: Color,
    val amber: Color,
    val lemon: Color,
    val lime: Color,
    val cyan: Color,
    val blue: Color,
    val violet: Color,
    val pink: Color,
    val mint: Color,
) {
    /** 黑玻璃/白玻璃面板底 */
    val glassPanel: Color get() = if (isDark) Color(0x08FFFFFF) else Color(0xD9FFFFFF)
    val glassPanelStrong: Color get() = if (isDark) Color(0x0FFFFFFF) else Color(0x99FFFFFF)
    val glassBorder: Color get() = if (isDark) Color(0x12FFFFFF) else Color(0xCCFFFFFF)
    val heroGrad: Brush
        get() = if (isDark) {
            horizontalGradient(listOf(Color(0xFFFF7A59), Color(0xFFC77DFF), Color(0xFF5CE1E6)))
        } else {
            horizontalGradient(listOf(Color(0xFFD94F2E), Color(0xFF8A3FD0), Color(0xFF0E8A99)))
        }
    val glineGrad: Brush
        get() = if (isDark) {
            horizontalGradient(listOf(Color(0xFFFF7A59), Color(0xFFC77DFF)))
        } else {
            horizontalGradient(listOf(Color(0xFFD94F2E), Color(0xFF8A3FD0)))
        }
    val dockGrad: Brush
        get() = if (isDark) {
            horizontalGradient(listOf(Color(0xFFFF7A59), Color(0xFFC77DFF)))
        } else {
            horizontalGradient(listOf(Color(0xFFD94F2E), Color(0xFF8A3FD0)))
        }
    val switchGrad: Brush
        get() = if (isDark) {
            horizontalGradient(listOf(Color(0xFFFF7A59), Color(0xFFC77DFF)))
        } else {
            horizontalGradient(listOf(Color(0xFFD94F2E), Color(0xFF8A3FD0)))
        }
    val togOff: Color get() = if (isDark) Color(0xFF2C2C38) else Color(0xFFD9D6E6)
    val togThumbOff: Color get() = if (isDark) Color(0xFF8E8B9C) else Color.White

    /** 屏幕背景光晕（.screen::before 三组径向渐变，a1 变体）。
     *  设计稿：300×240 at 15% 8% / 280×240 at 90% 30% / 280×240 at 40% 95%，blur 64px，opacity .75 */
    fun auraBrush(seed: Int): Brush {
        val colors = if (isDark) {
            when (seed % 4) {
                0 -> listOf(Color(0xFF5C2B1E), Color(0xFF2B1E5C), Color(0xFF1E4230))
                1 -> listOf(Color(0xFF1E3A5C), Color(0xFF5C1E47), Color(0xFF1E4242))
                2 -> listOf(Color(0xFF1E4242), Color(0xFF3A2B5C), Color(0xFF5C2B1E))
                else -> listOf(Color(0xFF5C2B47), Color(0xFF42421E), Color(0xFF2B1E5C))
            }
        } else {
            when (seed % 4) {
                0 -> listOf(Color(0xFFFFD3C2), Color(0xFFDDD0FF), Color(0xFFC2ECD4))
                1 -> listOf(Color(0xFFC9DEFC), Color(0xFFFCC9E3), Color(0xFFC2F0EC))
                2 -> listOf(Color(0xFFC2F0EC), Color(0xFFDED0FC), Color(0xFFFFD9C4))
                else -> listOf(Color(0xFFFBD0E3), Color(0xFFFDF0C4), Color(0xFFD4D2FC))
            }
        }
        // 三个独立光斑，用横向渐变近似三色并列（Compose 单 Brush 无法叠三组径向，
        // 故在外层由 auraLayers() 叠加三层）。
        return Brush.horizontalGradient(colors)
    }

    /** 三层独立径向光斑（对应设计稿三个 radial-gradient），按坐标叠加 */
    fun auraLayers(seed: Int): List<Pair<Brush, androidx.compose.ui.geometry.Offset>> {
        val colors = if (isDark) {
            when (seed % 4) {
                0 -> listOf(Color(0xFF5C2B1E), Color(0xFF2B1E5C), Color(0xFF1E4230))
                1 -> listOf(Color(0xFF1E3A5C), Color(0xFF5C1E47), Color(0xFF1E4242))
                2 -> listOf(Color(0xFF1E4242), Color(0xFF3A2B5C), Color(0xFF5C2B1E))
                else -> listOf(Color(0xFF5C2B47), Color(0xFF42421E), Color(0xFF2B1E5C))
            }
        } else {
            when (seed % 4) {
                0 -> listOf(Color(0xFFFFD3C2), Color(0xFFDDD0FF), Color(0xFFC2ECD4))
                1 -> listOf(Color(0xFFC9DEFC), Color(0xFFFCC9E3), Color(0xFFC2F0EC))
                2 -> listOf(Color(0xFFC2F0EC), Color(0xFFDED0FC), Color(0xFFFFD9C4))
                else -> listOf(Color(0xFFFBD0E3), Color(0xFFFDF0C4), Color(0xFFD4D2FC))
            }
        }
        val alpha = if (isDark) 0.5f else 1f
        val positions = listOf(
            androidx.compose.ui.geometry.Offset(0.15f, 0.08f),
            androidx.compose.ui.geometry.Offset(0.90f, 0.30f),
            androidx.compose.ui.geometry.Offset(0.40f, 0.95f),
        )
        return colors.mapIndexed { i, c ->
            Brush.radialGradient(
                colors = listOf(c.copy(alpha = alpha), Color.Transparent),
                center = positions[i],
                radius = 750f,
            ) to positions[i]
        }
    }
}

@Composable
fun glassTokens(): GlassTokens {
    val dark = isNightScheme()
    return if (dark) {
        GlassTokens(
            isDark = true,
            bg = Color(0xFF0D0D14), panel = Color(0xFF16161F), panel2 = Color(0xFF1C1C28),
            line = Color(0x12FFFFFF),
            ink = Color(0xFFF0EEF6), ink2 = Color(0xFFC9C6D6), sub = Color(0xFF8E8B9C), faint = Color(0xFF55525F),
            flame = Color(0xFFFF7A59), amber = Color(0xFFFFB35C), lemon = Color(0xFFFFE45C), lime = Color(0xFF6EF08A),
            cyan = Color(0xFF5CE1E6), blue = Color(0xFF7AA2FF), violet = Color(0xFFC77DFF), pink = Color(0xFFFF7AC8),
            mint = Color(0xFF6EF0C0),
        )
    } else {
        GlassTokens(
            isDark = false,
            bg = Color(0xFFEEF0F7), panel = Color(0x8CFFFFFF), panel2 = Color(0x8CFFFFFF),
            line = Color(0xCCFFFFFF),
            ink = Color(0xFF26262E), ink2 = Color(0xFF55525F), sub = Color(0xFF7A7788), faint = Color(0xFFA3A0B2),
            flame = Color(0xFFD94F2E), amber = Color(0xFFC97A10), lemon = Color(0xFFA8860E), lime = Color(0xFF2E9A50),
            cyan = Color(0xFF0E8A99), blue = Color(0xFF4D6FD8), violet = Color(0xFF8A3FD0), pink = Color(0xFFD84F9A),
            mint = Color(0xFF1E9A78),
        )
    }
}
