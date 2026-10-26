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
    /** 画布底色：设计稿那种带一点紫灰的浅色。 */
    val CanvasLight = Color(0xFFF5F4F9)
    val CanvasDark = Color(0xFF101014)

    // —— 主页各卡片的底色 / 内容色（浅色模式，与设计稿逐色对照）——
    /** 主状态卡：粉桃渐变（135deg #FDE4E0 → #FBD8E4）。 */
    val HeroGradientStart = Color(0xFFFDE4E0)
    val HeroGradientEnd = Color(0xFFFBD8E4)
    /** 主卡文字墨色与强调色（薰衣草紫）。 */
    val HeroContent = Color(0xFF2B2B33)
    val HeroAccent = Color(0xFF6C5FC7)
    /** 状态章描边玫红。 */
    val StatusRose = Color(0xFFC2567E)
    /** 兼容旧引用（作者页头像卡 / 重启弹层图标底）：淡紫。 */
    val HeroSurface = Color(0xFFE4DEF8)

    /** KPI 四格：设备型号=薄荷 / 界面风格=奶油 / 超级用户=淡紫 / 模块=玫瑰。 */
    val KpiMint = Color(0xFFDDEEDC) to Color(0xFF3E7A4C)
    val KpiCream = Color(0xFFFAEDCF) to Color(0xFF9A7A1E)
    val KpiLilac = Color(0xFFE4DEF8) to Color(0xFF6C5FC7)
    val KpiRose = Color(0xFFF7DFE9) to Color(0xFFB0508A)

    /** 工具胶囊：SELinux=薄荷 / Seccomp=灰 / 支持开发=蜜桃 / 了解 KernelSU=天蓝。 */
    val PillMint = Color(0xFFDDEEDC) to Color(0xFF3E7A4C)
    val PillGray = Color(0xFFECECF2) to Color(0xFF55525F)
    val PillPeach = Color(0xFFFBE3D6) to Color(0xFFB06A3A)
    val PillSky = Color(0xFFDDE9F7) to Color(0xFF3F6AA6)
    val PillLilac = Color(0xFFE4DEF8) to Color(0xFF6C5FC7)
    val PillSand = Color(0xFFFAEDCF) to Color(0xFF9A7A1E)

    // —— 深色模式：同样的色相压暗，配亮字 ——
    val HeroSurfaceDark = Color(0xFF2A2140)
    val HeroContentDark = Color(0xFFE8DFFF)
    val HeroAccentDark = Color(0xFFC4ACF5)

    val KpiMintDark = Color(0xFF172A1B) to Color(0xFFBFE8C6)
    val KpiCreamDark = Color(0xFF2E2716) to Color(0xFFEFD79A)
    val KpiLilacDark = Color(0xFF221C3A) to Color(0xFFCDBDF5)
    val KpiRoseDark = Color(0xFF2E1A22) to Color(0xFFF2BCCD)

    val PillMintDark = Color(0xFF172A1B) to Color(0xFFBFE8C6)
    val PillGrayDark = Color(0xFF23222B) to Color(0xFFC6C3D2)
    val PillPeachDark = Color(0xFF33241A) to Color(0xFFF0C4A0)
    val PillSkyDark = Color(0xFF1A2436) to Color(0xFFB9CDEF)
    val PillLilacDark = Color(0xFF241E3D) to Color(0xFFC9BAEF)
    val PillSandDark = Color(0xFF2C2616) to Color(0xFFE8D296)

    // —— 底栏（设计稿三页同款）——
    /** 未选中图标 / 文字。 */
    val NavIdle = Color(0xFFA5A2B5)
    /** 选中色 + 选中底。 */
    val NavActive = Color(0xFF6C5FC7)
    val NavActiveBg = Color(0xFFF3F0FB)
    /** 角标：薄荷绿底白字。 */
    val NavBadge = Color(0xFF3E7A4C)

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
