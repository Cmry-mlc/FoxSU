package me.weishu.kernelsu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 参考图（foxsu-ui-refined）的设计规范：主页 / 设置 / 重启弹层共用同一套
 * 柔和马卡龙配色、圆角、留白与图标规格，保证几个页面看起来是同一个 App。
 *
 * 配色规则（与参考图逐色对照）：
 *  - 画布：极浅的灰紫 / 灰蓝，卡片浮在上面
 *  - 主卡（工作中）：淡薰衣草紫 + 薰衣草色图标
 *  - KPI 四格：薄荷绿 / 奶油黄 / 淡紫 / 淡粉，逐格不同色
 *  - 工具胶囊：浅紫、薄荷、浅黄、淡紫，图标色比底色深
 *  - 文字：标题近黑（#1B1B1F），说明文字 12~13sp 半透明
 */
object UiPalette {
    /** 画布底色：参考图那种带一点紫灰的浅色。 */
    val CanvasLight = Color(0xFFF6F4FB)
    val CanvasDark = Color(0xFF101014)

    // —— 主页各卡片的底色 / 内容色（浅色模式）——
    /** 主状态卡：淡薰衣草。 */
    val HeroSurface = Color(0xFFEDE4FC)
    val HeroContent = Color(0xFF2A1B45)
    val HeroAccent = Color(0xFF6E4BD4)

    /** KPI 四格：设备型号 / 界面风格 / 超级用户 / 模块。 */
    val KpiMint = Color(0xFFDDF3DE) to Color(0xFF1D4526)
    val KpiCream = Color(0xFFFBEEDA) to Color(0xFF4A3710)
    val KpiLilac = Color(0xFFE6DFFA) to Color(0xFF2E2148)
    val KpiRose = Color(0xFFFBE0EA) to Color(0xFF4A1F31)

    /** 工具胶囊：SELinux / Seccomp / 支持开发 / 了解 KernelSU。 */
    val PillLilac = Color(0xFFE9E1FB) to Color(0xFF3A2A63)
    val PillSand = Color(0xFFFCF0CE) to Color(0xFF4A3710)
    val PillMint = Color(0xFFE3F5E0) to Color(0xFF1D4526)
    val PillSky = Color(0xFFE7E5FA) to Color(0xFF2E2A63)

    // —— 深色模式：同样的色相压暗，配亮字 ——
    val HeroSurfaceDark = Color(0xFF2A2140)
    val HeroContentDark = Color(0xFFE8DFFF)
    val HeroAccentDark = Color(0xFFC4ACF5)

    val KpiMintDark = Color(0xFF172A1B) to Color(0xFFBFE8C6)
    val KpiCreamDark = Color(0xFF2E2716) to Color(0xFFEFD79A)
    val KpiLilacDark = Color(0xFF221C3A) to Color(0xFFCDBDF5)
    val KpiRoseDark = Color(0xFF2E1A22) to Color(0xFFF2BCCD)

    val PillLilacDark = Color(0xFF241E3D) to Color(0xFFC9BAEF)
    val PillSandDark = Color(0xFF2C2616) to Color(0xFFE8D296)
    val PillMintDark = Color(0xFF172A1B) to Color(0xFFBFE8C6)
    val PillSkyDark = Color(0xFF1F1D38) to Color(0xFFC3BEF2)

    // —— 重启弹层用的柔和色（与主页同一套马卡龙）——
    val RebootTones = listOf(
        Color(0xFFE9E1FB) to Color(0xFF3A2A63),
        Color(0xFFE3F5E0) to Color(0xFF1D4526),
        Color(0xFFFCF0CE) to Color(0xFF4A3710),
        Color(0xFFFBE0EA) to Color(0xFF4A1F31),
        Color(0xFFDDEBFA) to Color(0xFF17324D),
        Color(0xFFDFF3F3) to Color(0xFF0F3A3A),
    )

    // —— 形状与尺寸 ——
    /** 大卡片圆角（主卡 / KPI 卡）。 */
    val CardShape = RoundedCornerShape(24.dp)
    /** 工具胶囊圆角：完全药丸。 */
    val PillShape = RoundedCornerShape(26.dp)
    /** 图标底板圆角。 */
    val IconBoxShape = RoundedCornerShape(12.dp)

    /** 卡片间距。 */
    val CardGap = 12.dp
    /** 页面左右留白。 */
    val PagePadding = 16.dp
}
