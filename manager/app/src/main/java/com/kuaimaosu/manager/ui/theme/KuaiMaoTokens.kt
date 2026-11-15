package com.kuaimaosu.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject

/**
 * KuaiMaoSU · XDesign 设计令牌。
 *
 * 与交付包 css/tokens.css / design-tokens.json 一一对应。
 * 颜色分为浅色 / 深色两套调色板，由 [LocalKuaiMaoPalette] 在主题根节点注入，
 * 组件统一通过 [KuaiMaoColors] 取色，所以暗夜模式下不需要改任何组件代码。
 */
@Immutable
data class KuaiMaoPalette(
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
    // 图标底板：全站统一的图标底色，前景统一取 brand
    val iconContainer: Color,
)

/** 浅色：设计稿基准色板 */
val KuaiMaoLightPalette = KuaiMaoPalette(
    // 全站统一画布：#F0F1F3（对齐系统设置的浅灰底）。所有页面、二级页、弹层
    // 都共用这一块底色，背景特效直接画在它上面，页面自己不再铺任何实色底。
    canvasStage = Color(0xFFF0F1F3),
    canvasApp = Color(0xFFF0F1F3),
    // 卡片必须完全不透明：背景特效已经垫在页面之下，卡片再留通透度只会
    // 让底下那层底色透上来（看着发灰、字发虚），弹层与下拉菜单还会重影。
    canvasCard = Color(0xFFFFFFFF),
    canvasField = Color(0xFFF7F7F9),
    canvasSubtle = Color(0xFFF3F4F6),
    canvasHover = Color(0xFFEFF0F3),
    text = Color(0xFF111114),
    textBody = Color(0xFF1A1A1E),
    textLabel = Color(0xFF3C3C41),
    textSec = Color(0xFF5C5C62),
    // 次要文字按 WCAG 反推到白底至少 4.5:1：原来的 #9AA0B0 只有 2.6:1，
    // 12sp 的行描述、版本号、作者名整片发虚。
    textTer = Color(0xFF6B6B71),   // ≈4.9:1
    textMuted = Color(0xFF7A7A80), // ≈4.0:1
    textDisabled = Color(0xFFA6A6AC),
    // 默认强调色是中性石墨灰（对齐系统设置那种黑白灰观感），需要彩色时由
    // 用户自选主题色覆盖，不再默认给一个蓝紫。
    brand = Color(0xFF3C4048),
    brandSoft = Color(0xFFEFEFF2),
    brandDeep = Color(0xFF23262C),
    ink = Color(0xFF222226),
    inkLight = Color(0xFF3A3A40),
    inkDeep = Color(0xFF18181C),
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
    slate = Color(0xFF5C5C62),
    slateSoft = Color(0xFFF0F0F3),
    amber = Color(0xFFC07618),
    amberSoft = Color(0xFFFFF4E2),
    violet = Color(0xFF5A5F69),
    violetSoft = Color(0xFFEFEFF2),
    line = Color(0xFFEEEEF1),
    lineField = Color(0xFFE6E7EB),
    lineFocus = Color(0xFFC7C7CE),
    chevron = Color(0xFFB9B9BF),
    iconContainer = Color(0xFFF2F2F5),
)

/** 深色：纯黑画布 + 中性深灰卡面（系统设置那种夜间观感） */
val KuaiMaoDarkPalette = KuaiMaoPalette(
    // 夜间不再用深靛蓝：画布纯黑、卡片 #1C1C1C、描边 #2C2C2E，
    // 强调色也换成中性近白，整屏只有黑白灰，图标与状态色负责点缀。
    canvasStage = Color(0xFF000000),
    canvasApp = Color(0xFF000000),
    canvasCard = Color(0xFF1C1C1C),
    canvasField = Color(0xFF1C1C1C),
    canvasSubtle = Color(0xFF242424),
    canvasHover = Color(0xFF2C2C2E),
    text = Color(0xFFF5F5F7),
    textBody = Color(0xFFEDEDF0),
    textLabel = Color(0xFFD6D6DC),
    textSec = Color(0xFFB2B2BA),
    textTer = Color(0xFF9C9CA4),
    textMuted = Color(0xFF97979F),
    textDisabled = Color(0xFF5A5A62),
    brand = Color(0xFFF2F2F7),
    brandSoft = Color(0xFF2C2C2E),
    brandDeep = Color(0xFFFFFFFF),
    ink = Color(0xFFEDEDF0),
    inkLight = Color(0xFFF5F5F7),
    inkDeep = Color(0xFF101013),
    success = Color(0xFF4FD6A0),
    successSoft = Color(0xFF17251E),
    successDark = Color(0xFF7BEFC0),
    danger = Color(0xFFFF7A80),
    dangerSoft = Color(0xFF2A1416),
    warning = Color(0xFFE8A948),
    warningSoft = Color(0xFF2A2110),
    info = Color(0xFF7FA6FF),
    infoSoft = Color(0xFF151C2A),
    rose = Color(0xFFFF7A80),
    roseSoft = Color(0xFF2A1416),
    slate = Color(0xFFA9A9B2),
    slateSoft = Color(0xFF242426),
    amber = Color(0xFFE8A948),
    amberSoft = Color(0xFF2A2110),
    violet = Color(0xFFC9C9D2),
    violetSoft = Color(0xFF26262A),
    line = Color(0xFF2C2C2E),
    lineField = Color(0xFF35353A),
    lineFocus = Color(0xFF6C6C74),
    chevron = Color(0xFF5E5E66),
    iconContainer = Color(0xFF2C2C2E),
)

/** 当前主题调色板，在 [KernelSUTheme] 中注入 */
val LocalKuaiMaoPalette = staticCompositionLocalOf { KuaiMaoLightPalette }

/**
 * 「当前这一页的文字是不是画在深色底上」。
 *
 * 不光看主题深浅：开了自定义壁纸、而壁纸本身偏暗时，画在壁纸上的大标题、
 * 分组标题、副标题也得换成浅色，否则就是黑字压在暗花丛上，怎么调卡片透明度都看不清。
 * 由 NavContainer 按「壁纸平均亮度 + 蒙版浓度」算好注入。
 */
val LocalDarkBackdrop = staticCompositionLocalOf { false }

/**
 * 「当前这一页是不是画在自定义壁纸上面」。
 *
 * 和 [LocalDarkBackdrop] 的区别：那个回答「该用浅字还是深字」，这个回答
 * 「画在壁纸上的小字要不要加一层描边阴影」。壁纸本身就是一张照片，
 * 12sp 以下的小字压在花丛上，不管深浅都会糊；加一层反色软阴影就立刻清楚。
 */
val LocalOnWallpaper = staticCompositionLocalOf { false }

/** 直接画在背景（画布 / 壁纸 / 特效）上的主文字色。 */
@Composable
@ReadOnlyComposable
fun backdropInk(alpha: Float = 1f): Color =
    if (LocalDarkBackdrop.current) {
        Color.White.copy(alpha = alpha)
    } else {
        KuaiMaoColors.text.copy(alpha = alpha)
    }

/** 直接画在背景上的次要文字色（副标题、小字）。 */
@Composable
@ReadOnlyComposable
fun backdropInkSoft(alpha: Float = 1f): Color = when {
    // 壁纸上的小字不靠阴影，直接给足对比：亮底就是黑字，暗底就是白字。
    LocalDarkBackdrop.current -> Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
    LocalOnWallpaper.current -> KuaiMaoColors.textBody.copy(alpha = alpha.coerceIn(0f, 1f))
    else -> KuaiMaoColors.textTer.copy(alpha = alpha)
}

/** 直接画在背景上的强调文字色（“查看全部”这类小链接）。 */
@Composable
@ReadOnlyComposable
fun backdropAccentInk(): Color =
    if (LocalDarkBackdrop.current) {
        Color.White.copy(alpha = 0.88f)
    } else {
        KuaiMaoColors.brandDeep
    }

/**
 * 组件取色入口。
 *
 * 每个字段都是随主题重组的只读属性，读写方式与普通常量完全一致：
 * `KuaiMaoColors.canvasCard`。
 */
/** 当前调色板是不是浅色（只看主题，不看壁纸亮度）。 */
@Composable
@ReadOnlyComposable
private fun isLightSurface(): Boolean =
    LocalKuaiMaoPalette.current.canvasStage.luminance() > 0.5f

object KuaiMaoColors {
    val canvasStage: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.canvasStage
    val canvasApp: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.canvasApp
    /**
     * 卡片底：没开背景时是不透明卡片色；开了自定义壁纸或背景特效后，
     * 按源码那套规则变成 55%（可调）的半透明卡片，底下的壁纸/特效才透得上来。
     */
    val canvasCard: Color @Composable get() = kuaimaoCardTint()

    /** 弹层、菜单、底部弹窗用的实色卡片底：这些地方必须压住背后的内容。 */
    val canvasCardSolid: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.canvasCard
    /**
     * 输入框 / 内嵌小块的底：和卡片同一套透明规则。
     *
     * 以前这里恒为实色，开了自定义壁纸后搜索框就变成压在壁纸上的一块实心黑/白砖头，
     * 和旁边的半透明卡片完全不是一个东西。
     */
    val canvasField: Color @Composable get() = kuaimaoSurfaceTint(LocalKuaiMaoPalette.current.canvasField)
    /** 输入框的实色底，只给磨砂层当 tint 用。 */
    val canvasFieldSolid: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.canvasField
    val canvasSubtle: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.canvasSubtle
    val canvasHover: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.canvasHover

    val text: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.text
    val textBody: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.textBody
    val textLabel: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.textLabel
    val textSec: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.textSec
    /**
     * 壁纸上的次级文字不能再用常规那套浅灰（#7A7A80）：
     * 卡片只有 50% 不透明，底下的花丛直接顶上来，10sp 的说明文字会糊成一片。
     * 亮色壁纸下统一加深两档；夜间壁纸本来就是浅字，保持原值。
     */
    val textTer: Color @Composable @ReadOnlyComposable
        get() = if (LocalOnWallpaper.current && isLightSurface()) {
            LocalKuaiMaoPalette.current.textSec
        } else {
            LocalKuaiMaoPalette.current.textTer
        }
    val textMuted: Color @Composable @ReadOnlyComposable
        get() = if (LocalOnWallpaper.current && isLightSurface()) {
            LocalKuaiMaoPalette.current.textLabel
        } else {
            LocalKuaiMaoPalette.current.textMuted
        }
    val textDisabled: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.textDisabled

    val brand: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.brand
    val brandSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.brandSoft
    val brandDeep: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.brandDeep
    val ink: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.ink
    val inkLight: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.inkLight
    val inkDeep: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.inkDeep

    val success: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.success
    val successSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.successSoft
    val successDark: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.successDark
    val danger: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.danger
    val dangerSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.dangerSoft
    val warning: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.warning
    val warningSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.warningSoft
    val info: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.info
    val infoSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.infoSoft
    val rose: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.rose
    val roseSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.roseSoft
    val slate: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.slate
    val slateSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.slateSoft
    val amber: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.amber
    val amberSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.amberSoft
    val violet: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.violet
    val violetSoft: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.violetSoft

    val line: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.line
    val lineField: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.lineField
    val lineFocus: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.lineFocus
    val chevron: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.chevron
    val iconContainer: Color @Composable @ReadOnlyComposable get() = LocalKuaiMaoPalette.current.iconContainer
}

object KuaiMaoGradients {
    /** 内核状态大卡：深色卡面（两套主题通用） */
    val card = Brush.linearGradient(
        colors = listOf(Color(0xFF181D33), Color(0xFF232C56), Color(0xFF1B2440)),
    )

    val button = Brush.linearGradient(
        colors = listOf(Color(0xFF2A2A2E), Color(0xFF3C3C42)),
    )

    val avatar = Brush.linearGradient(
        colors = listOf(Color(0xFF1C1C1E), Color(0xFF2E2E32)),
    )

    /** 大卡光斑：右上紫、左下绿 */
    val glow = Brush.radialGradient(
        colors = listOf(Color(0x806C5CF0), Color.Transparent),
    )
}

object KuaiMaoDimens {
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
object KuaiMaoType {
    val kicker: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = backdropInkSoft(0.95f),
    )
    val greeting: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 17.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.2).sp,
        color = KuaiMaoColors.text,
    )
    val heroLabel: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.2.sp,
    )
    val heroState: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 31.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.6).sp,
    )
    val heroHint: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
    val heroMetricLabel: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.6.sp,
    )
    // 21sp Black 比后面 11sp 的单位大了一圈，视觉上只看见两个数字。
    // 收到 16sp / ExtraBold，和「已授权 / 已安装」才是一行字里的两个层级。
    val heroMetricValue: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp,
    )
    val sectionTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.2).sp,
        color = backdropInk(),
    )
    val sectionAction: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = backdropAccentInk(),
    )
    val rowTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = KuaiMaoColors.textBody,
    )
    val rowDesc: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = KuaiMaoColors.textMuted,
    )
    val rowValue: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
    )
    val quickLabel: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = KuaiMaoColors.textLabel,
    )
    val pageTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        color = backdropInk(),
    )
    val pageSub: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = backdropInkSoft(),
    )
    val navLabel: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = LocalKuaiMaoFontFamily.current,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.ExtraBold,
    )
}

/** KuaiMaoSU 浅色配色（XDesign 基调） */
val KuaiMaoLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF3C4048),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFEFF2),
    onPrimaryContainer = Color(0xFF23262C),
    inversePrimary = Color(0xFFC6C9D0),
    secondary = Color(0xFF5A5F69),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEFF2),
    onSecondaryContainer = Color(0xFF23262C),
    tertiary = Color(0xFF1CA672),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4F7EF),
    onTertiaryContainer = Color(0xFF0E6A49),
    background = Color(0xFFF0F1F3),
    onBackground = Color(0xFF101322),
    surface = Color(0xFFF0F1F3),
    onSurface = Color(0xFF101322),
    surfaceVariant = Color(0xFFF0F1F3),
    onSurfaceVariant = Color(0xFF5A6070),
    surfaceTint = Color(0xFF3C4048),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF0F1F3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF2F3F5),
    inverseSurface = Color(0xFF181D33),
    inverseOnSurface = Color(0xFFF2F4FA),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFE9EC),
    onErrorContainer = Color(0xFFB02A31),
    outline = Color(0xFFB9BFCC),
    outlineVariant = Color(0xFFEEEEF1),
    scrim = Color(0x70101322),
)

/** KuaiMaoSU 深色配色：纯黑画布 + 中性灰层级，与 [KuaiMaoDarkPalette] 同源 */
val KuaiMaoDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFF2F2F7),
    onPrimary = Color(0xFF101013),
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = Color(0xFFF5F5F7),
    secondary = Color(0xFFD6D6DC),
    onSecondary = Color(0xFF101013),
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color(0xFFF5F5F7),
    tertiary = Color(0xFF4FD6A0),
    onTertiary = Color(0xFF06301F),
    tertiaryContainer = Color(0xFF17251E),
    onTertiaryContainer = Color(0xFFB9F2DA),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFFB2B2BA),
    surfaceTint = Color(0xFFF2F2F7),
    surfaceBright = Color(0xFF343436),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2C2C2E),
    inverseSurface = Color(0xFFF5F5F7),
    inverseOnSurface = Color(0xFF101322),
    error = Color(0xFFFF7A80),
    onError = Color(0xFF3A0A0D),
    errorContainer = Color(0xFF3A1416),
    onErrorContainer = Color(0xFFFFDADB),
    outline = Color(0xFF5E5E66),
    outlineVariant = Color(0xFF2C2C2E),
    scrim = Color(0xCC000000),
)

/**
 * 图标块的「浅底 + 强调色」组合。
 *
 * 全站统一：底板固定用 [KuaiMaoColors.iconContainer]，前景固定用 [KuaiMaoColors.brand]，
 * 换主题色时所有图标一起变色，不再出现紫/橙/绿混排的彩色底。
 * 语义色仍保留给状态文字（如 SELinux 的“强制/宽容”），只是不再染色图标块。
 */
@Immutable
data class KuaiMaoTone(
    val content: Color,
    val container: Color,
)

object KuaiMaoTones {
    /**
     * 全站统一的图标配色：底板是主题色的 12%（深色 22%）玻璃，前景是同色系加深/提亮后的实色。
     *
     * 之前底板固定 #EFF1F8、前景固定 brand，遇到浅色主题（尤其灰色主题）时
     * 图标和底板几乎同亮度，看着就是「一团糊」。这里改成跟随
     * [MaterialTheme.colorScheme.primary]，并按明暗自动拉开对比；主题色本身太浅/太深时
     * 再压一档，保证任何颜色下图标都看得清。
     */
    val unified: KuaiMaoTone
        @Composable @ReadOnlyComposable
        get() {
            val accent = MaterialTheme.colorScheme.primary
            val isDark = LocalKuaiMaoPalette.current.canvasCard.luminance() < 0.5f
            return KuaiMaoTone(
                content = accentIconInk(accent, isDark),
                container = accent.copy(alpha = if (isDark) 0.24f else 0.14f),
            )
        }

    /**
     * 图标前景：亮底压深、暗底提亮，并保证亮度落在可读区间。
     *
     * 之前只压 30% 黑，灰色主题（#71717A）出来的图标是中灰，配上 12% 的浅灰底，
     * 实际对比只有 3:1 出头，用户连着几版都说「图标看不清」。现在直接压到
     * 亮度 0.13 以下（深色主题提到 0.68 以上），白卡上对比约 12:1，缩到 20dp 也不糊。
     */
    fun accentIconInk(accent: Color, isDark: Boolean): Color = if (isDark) {
        var ink = lerp(accent, Color.White, 0.55f)
        if (ink.luminance() < 0.68f) ink = lerp(accent, Color.White, 0.86f)
        ink
    } else {
        var ink = lerp(accent, Color.Black, 0.45f)
        if (ink.luminance() > 0.13f) ink = lerp(accent, Color.Black, 0.72f)
        ink
    }

    val violet: KuaiMaoTone @Composable @ReadOnlyComposable get() = unified
    val rose: KuaiMaoTone @Composable @ReadOnlyComposable get() = unified
    val blue: KuaiMaoTone @Composable @ReadOnlyComposable get() = unified
    val slate: KuaiMaoTone @Composable @ReadOnlyComposable get() = unified
    val green: KuaiMaoTone @Composable @ReadOnlyComposable get() = unified
    val amber: KuaiMaoTone @Composable @ReadOnlyComposable get() = unified
    val danger: KuaiMaoTone @Composable @ReadOnlyComposable get() = unified
    val neutral: KuaiMaoTone @Composable @ReadOnlyComposable get() = unified
}


/**
 * 卡片当前该用的颜色。
 *
 * - 没有背景（壁纸与特效都关）：不透明白卡，和以前完全一致；
 * - 开了自定义壁纸：不透明度取用户设置（默认 0.55），壁纸透过来；
 * - 只开了背景特效：浅色 0.55 / 深色 0.66，特效从卡片底下透出来但字仍然清楚。
 */
@Composable
fun kuaimaoCardTint(): Color = kuaimaoSurfaceTint(LocalKuaiMaoPalette.current.canvasCard)

/**
 * 卡片 / 输入框 / 内嵌面共用的透明度规则。
 *
 * 只有「自定义壁纸」或「背景特效」开着的时候才会变半透明；两者都关就是实色，
 * 所以老配置里残留的 0.5 不会把没有壁纸的页面弄透。
 */
@Composable
fun kuaimaoSurfaceTint(base: Color): Color {
    val cardConfig: CardConfig = koinInject()
    val themeConfig: ThemeConfig = koinInject()
    val alpha = when {
        // 壁纸几乎没模糊时，卡片再按 50% 透过去，字压花丛上会糊成一片，
        // 这里兜底抬到 72%，模糊拉起来之后再回到用户设定。
        cardConfig.isCustomBackgroundEnabled ->
            if (themeConfig.backgroundBlur < 0.12f) {
                maxOf(cardConfig.cardAlpha, 0.72f)
            } else {
                cardConfig.cardAlpha
            }

        themeConfig.backgroundEffect != BackgroundEffect.NONE -> {
            if (base.luminance() < 0.5f) 0.66f else 0.55f
        }

        else -> 1f
    }
    return base.copy(alpha = alpha.coerceIn(0f, 1f))
}
