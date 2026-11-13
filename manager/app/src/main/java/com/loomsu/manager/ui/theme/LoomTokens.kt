package com.loomsu.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Loom SU · XDesign 设计令牌。
 *
 * 与交付包 css/tokens.css / design-tokens.json 一一对应。
 * 颜色分为浅色 / 深色两套调色板，由 [LocalLoomPalette] 在主题根节点注入，
 * 组件统一通过 [LoomColors] 取色，所以暗夜模式下不需要改任何组件代码。
 */
@Immutable
data class LoomPalette(
    // 画布
    val canvasStage: Color,
    val canvasApp: Color,
    val canvasCard: Color,
    val canvasField: Color,
    val canvasSubtle: Color,
    val canvasHover: Color,
    // 文字
    val text: Color,
    val textBody: Color,
    val textLabel: Color,
    val textSec: Color,
    val textTer: Color,
    val textMuted: Color,
    val textDisabled: Color,
    // 品牌
    val brand: Color,
    val brandSoft: Color,
    val brandDeep: Color,
    val ink: Color,
    val inkLight: Color,
    val inkDeep: Color,
    // 语义
    val success: Color,
    val successSoft: Color,
    val successDark: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val rose: Color,
    val roseSoft: Color,
    val slate: Color,
    val slateSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val violet: Color,
    val violetSoft: Color,
    // 描边
    val line: Color,
    val lineField: Color,
    val lineFocus: Color,
    val chevron: Color,
)

/** 浅色：设计稿基准色板 */
val LoomLightPalette = LoomPalette(
    canvasStage = Color(0xFFE9EBEF),
    canvasApp = Color(0xFFF4F5F8),
    canvasCard = Color(0xFFFFFFFF),
    canvasField = Color(0xFFFAFBFC),
    canvasSubtle = Color(0xFFF4F5F8),
    canvasHover = Color(0xFFF1F2F6),
    text = Color(0xFF101322),
    textBody = Color(0xFF151A2C),
    textLabel = Color(0xFF3A4154),
    textSec = Color(0xFF5A6070),
    textTer = Color(0xFF8A90A0),
    textMuted = Color(0xFF9AA0B0),
    textDisabled = Color(0xFFA6ACBC),
    brand = Color(0xFF4F5BE8),
    brandSoft = Color(0xFFEEEBFF),
    brandDeep = Color(0xFF6B57E0),
    ink = Color(0xFF222945),
    inkLight = Color(0xFF333D6E),
    inkDeep = Color(0xFF181D33),
    success = Color(0xFF1CA672),
    successSoft = Color(0xFFE4F7EF),
    successDark = Color(0xFF7BEFC0),
    danger = Color(0xFFE5484D),
    dangerSoft = Color(0xFFFFE9EC),
    warning = Color(0xFFC07618),
    warningSoft = Color(0xFFFFF4E2),
    info = Color(0xFF3B6FE0),
    infoSoft = Color(0xFFE8F0FF),
    rose = Color(0xFFE5484D),
    roseSoft = Color(0xFFFFE9EC),
    slate = Color(0xFF5A6070),
    slateSoft = Color(0xFFF0F1F6),
    amber = Color(0xFFC07618),
    amberSoft = Color(0xFFFFF4E2),
    violet = Color(0xFF6B57E0),
    violetSoft = Color(0xFFEEEBFF),
    line = Color(0xFFF1F2F6),
    lineField = Color(0xFFEDEEF3),
    lineFocus = Color(0xFFC9C3F5),
    chevron = Color(0xFFB9BFCC),
)

/** 深色：同一套语义层级，只把明度关系倒过来 */
val LoomDarkPalette = LoomPalette(
    // 暗夜不是纯黑：沿用大卡的深靛色相，避免和紫蓝品牌色脱节
    canvasStage = Color(0xFF0F1428),
    canvasApp = Color(0xFF151B33),
    canvasCard = Color(0xFF1C2340),
    canvasField = Color(0xFF191F38),
    canvasSubtle = Color(0xFF232B4C),
    canvasHover = Color(0xFF283152),
    text = Color(0xFFF2F4FA),
    textBody = Color(0xFFE6E9F2),
    textLabel = Color(0xFFC9CEDD),
    textSec = Color(0xFFA9B0C4),
    textTer = Color(0xFF8A90A0),
    textMuted = Color(0xFF7C8398),
    textDisabled = Color(0xFF5A6180),
    brand = Color(0xFFA9B0FF),
    brandSoft = Color(0xFF2A2F55),
    brandDeep = Color(0xFFC6C9FF),
    ink = Color(0xFF222945),
    inkLight = Color(0xFF333D6E),
    inkDeep = Color(0xFF181D33),
    success = Color(0xFF4FD6A0),
    successSoft = Color(0xFF16382C),
    successDark = Color(0xFF7BEFC0),
    danger = Color(0xFFFF8A8F),
    dangerSoft = Color(0xFF3A1A1F),
    warning = Color(0xFFE8A948),
    warningSoft = Color(0xFF3A2A12),
    info = Color(0xFF7FA6FF),
    infoSoft = Color(0xFF1B2A4A),
    rose = Color(0xFFFF8A8F),
    roseSoft = Color(0xFF3A1A1F),
    slate = Color(0xFFA9B0C4),
    slateSoft = Color(0xFF232A44),
    amber = Color(0xFFE8A948),
    amberSoft = Color(0xFF3A2A12),
    violet = Color(0xFFA9B0FF),
    violetSoft = Color(0xFF2A2F55),
    line = Color(0xFF262D45),
    lineField = Color(0xFF2C3350),
    lineFocus = Color(0xFF4A4C8F),
    chevron = Color(0xFF5A6180),
)

/** 当前主题调色板，在 [KernelSUTheme] 中注入 */
val LocalLoomPalette = staticCompositionLocalOf { LoomLightPalette }

/**
 * 组件取色入口。
 *
 * 每个字段都是随主题重组的只读属性，读写方式与普通常量完全一致：
 * `LoomColors.canvasCard`。
 */
object LoomColors {
    val canvasStage: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.canvasStage
    val canvasApp: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.canvasApp
    val canvasCard: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.canvasCard
    val canvasField: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.canvasField
    val canvasSubtle: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.canvasSubtle
    val canvasHover: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.canvasHover

    val text: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.text
    val textBody: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.textBody
    val textLabel: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.textLabel
    val textSec: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.textSec
    val textTer: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.textTer
    val textMuted: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.textMuted
    val textDisabled: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.textDisabled

    val brand: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.brand
    val brandSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.brandSoft
    val brandDeep: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.brandDeep
    val ink: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.ink
    val inkLight: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.inkLight
    val inkDeep: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.inkDeep

    val success: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.success
    val successSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.successSoft
    val successDark: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.successDark
    val danger: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.danger
    val dangerSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.dangerSoft
    val warning: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.warning
    val warningSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.warningSoft
    val info: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.info
    val infoSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.infoSoft
    val rose: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.rose
    val roseSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.roseSoft
    val slate: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.slate
    val slateSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.slateSoft
    val amber: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.amber
    val amberSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.amberSoft
    val violet: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.violet
    val violetSoft: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.violetSoft

    val line: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.line
    val lineField: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.lineField
    val lineFocus: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.lineFocus
    val chevron: Color @Composable @ReadOnlyComposable get() = LocalLoomPalette.current.chevron
}

object LoomGradients {
    /** 内核状态大卡：深色卡面（两套主题通用） */
    val card = Brush.linearGradient(
        colors = listOf(Color(0xFF181D33), Color(0xFF232C56), Color(0xFF1B2440)),
    )

    val button = Brush.linearGradient(
        colors = listOf(Color(0xFF222945), Color(0xFF333D6E)),
    )

    val avatar = Brush.linearGradient(
        colors = listOf(Color(0xFF1A1F35), Color(0xFF2B3568)),
    )

    /** 大卡光斑：右上紫、左下绿 */
    val glow = Brush.radialGradient(
        colors = listOf(Color(0x806C5CF0), Color.Transparent),
    )
}

object LoomDimens {
    val sp1 = 4.dp
    val sp2 = 6.dp
    val sp3 = 8.dp
    val sp4 = 10.dp
    val sp5 = 12.dp
    val sp6 = 14.dp
    val sp7 = 16.dp
    val sp8 = 18.dp
    val sp9 = 22.dp

    val rXs = RoundedCornerShape(8.dp)
    val rSm = RoundedCornerShape(12.dp)
    val rMd = RoundedCornerShape(14.dp)
    val rLg = RoundedCornerShape(16.dp)
    val rXl = RoundedCornerShape(20.dp)
    val rCard = RoundedCornerShape(22.dp)
    val rHero = RoundedCornerShape(26.dp)
    val rSheet = RoundedCornerShape(28.dp)
    val rPill = RoundedCornerShape(100.dp)

    /** 页面左右安全边距 */
    val pageGutter = 20.dp

    /** 底栏占位高度（浮岛 + 外边距） */
    val navHeight = 66.dp
    val navInset = 16.dp
}

/** 文案样式；颜色跟随当前调色板。 */
object LoomType {
    val kicker: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = LoomColors.textMuted,
    )
    val greeting: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.2).sp,
        color = LoomColors.text,
    )
    val heroLabel: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.2.sp,
    )
    val heroState: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 31.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.6).sp,
    )
    val heroHint: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
    val heroMetricLabel: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.6.sp,
    )
    val heroMetricValue: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 21.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.3).sp,
    )
    val sectionTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.2).sp,
        color = LoomColors.text,
    )
    val sectionAction: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = LoomColors.brandDeep,
    )
    val rowTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = LoomColors.textBody,
    )
    val rowDesc: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = LoomColors.textMuted,
    )
    val rowValue: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
    )
    val quickLabel: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = LoomColors.textLabel,
    )
    val pageTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        color = LoomColors.text,
    )
    val pageSub: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LoomColors.textMuted,
    )
    val navLabel: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontSize = 8.5.sp,
        fontWeight = FontWeight.ExtraBold,
    )
}

/** Loom SU 浅色配色（XDesign 基调） */
val LoomLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF4F5BE8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEEBFF),
    onPrimaryContainer = Color(0xFF6B57E0),
    inversePrimary = Color(0xFFB9BEFF),
    secondary = Color(0xFF6B57E0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEBFF),
    onSecondaryContainer = Color(0xFF6B57E0),
    tertiary = Color(0xFF1CA672),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4F7EF),
    onTertiaryContainer = Color(0xFF0E6A49),
    background = Color(0xFFF4F5F8),
    onBackground = Color(0xFF101322),
    surface = Color(0xFFF4F5F8),
    onSurface = Color(0xFF101322),
    surfaceVariant = Color(0xFFF4F5F8),
    onSurfaceVariant = Color(0xFF5A6070),
    surfaceTint = Color(0xFF4F5BE8),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF4F5F8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF1F2F6),
    inverseSurface = Color(0xFF181D33),
    inverseOnSurface = Color(0xFFF2F4FA),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFE9EC),
    onErrorContainer = Color(0xFFB02A31),
    outline = Color(0xFFB9BFCC),
    outlineVariant = Color(0xFFF1F2F6),
    scrim = Color(0x70101322),
)

/** Loom SU 深色配色（与 [LoomDarkPalette] 同源） */
val LoomDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFA9B0FF),
    onPrimary = Color(0xFF1A1F42),
    primaryContainer = Color(0xFF2A2F55),
    onPrimaryContainer = Color(0xFFD7D9FF),
    secondary = Color(0xFFC6C9FF),
    onSecondary = Color(0xFF1A1F42),
    secondaryContainer = Color(0xFF2A2F55),
    onSecondaryContainer = Color(0xFFD7D9FF),
    tertiary = Color(0xFF4FD6A0),
    onTertiary = Color(0xFF06301F),
    tertiaryContainer = Color(0xFF173A2C),
    onTertiaryContainer = Color(0xFFB9F2DA),
    background = Color(0xFF151B33),
    onBackground = Color(0xFFF2F4FA),
    surface = Color(0xFF151B33),
    onSurface = Color(0xFFF2F4FA),
    surfaceVariant = Color(0xFF1A2038),
    onSurfaceVariant = Color(0xFFA9B0C4),
    surfaceTint = Color(0xFFA9B0FF),
    surfaceBright = Color(0xFF283152),
    surfaceDim = Color(0xFF131830),
    surfaceContainerLowest = Color(0xFF10152B),
    surfaceContainerLow = Color(0xFF191F38),
    surfaceContainer = Color(0xFF1C2340),
    surfaceContainerHigh = Color(0xFF232B4C),
    surfaceContainerHighest = Color(0xFF2B3458),
    inverseSurface = Color(0xFFF2F4FA),
    inverseOnSurface = Color(0xFF101322),
    error = Color(0xFFFF8A8F),
    onError = Color(0xFF4A0B10),
    errorContainer = Color(0xFF5A1720),
    onErrorContainer = Color(0xFFFFDADB),
    outline = Color(0xFF5A6180),
    outlineVariant = Color(0xFF262D45),
    scrim = Color(0xCC000000),
)

/** 语义色的「浅底 + 深字」组合，用于图标块与状态胶囊 */
@Immutable
data class LoomTone(
    val content: Color,
    val container: Color,
)

object LoomTones {
    val violet: LoomTone @Composable @ReadOnlyComposable
        get() = LoomTone(LoomColors.violet, LoomColors.violetSoft)
    val rose: LoomTone @Composable @ReadOnlyComposable
        get() = LoomTone(LoomColors.rose, LoomColors.roseSoft)
    val blue: LoomTone @Composable @ReadOnlyComposable
        get() = LoomTone(LoomColors.info, LoomColors.infoSoft)
    val slate: LoomTone @Composable @ReadOnlyComposable
        get() = LoomTone(LoomColors.slate, LoomColors.slateSoft)
    val green: LoomTone @Composable @ReadOnlyComposable
        get() = LoomTone(LoomColors.success, LoomColors.successSoft)
    val amber: LoomTone @Composable @ReadOnlyComposable
        get() = LoomTone(LoomColors.amber, LoomColors.amberSoft)
    val danger: LoomTone @Composable @ReadOnlyComposable
        get() = LoomTone(LoomColors.danger, LoomColors.dangerSoft)
}
