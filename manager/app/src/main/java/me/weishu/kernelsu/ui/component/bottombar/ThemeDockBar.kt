package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.LocalMainPagerState

/** 各主题的抽屉胶囊配色（设计语言驱动） */
data class DockTheme(
    val bg: Color,              // 胶囊底
    val border: Color,          // 描边
    val shadow: Color,          // 软阴影
    val selected: Color,        // 选中强调色
    val unselected: Color,      // 未选中文字/图标
    val selectedBg: Color,      // 选中底衬
    val badge: Color,           // 徽标底
    val glass: Boolean = false, // 顶部高光（沉浸光感）
    val backdrop: Color? = null, // 槽位底色（与页面底部背景同色，杜绝窗口白底透出）
)

/**
 * 底栏样式：设计稿 9 种（d0/v1/v5-v11）+ 由其他皮肤原生底栏复刻的 4 种
 * （editorial 纸面墨线 / pages 白色药丸 / glass 液态药丸 / nocturne 暗夜霓虹）。
 * 全部皮肤共用这一套选择器，样式全局生效。
 */
enum class DockStyle { D0, V1, V5, V6, V7, V8, V9, V10, V11, EDITORIAL, PAGES, GLASS, NOCTURNE;

    /** 是否为「复刻自其他皮肤」的样式（这些不参与 V 系列的项宽/指示器逻辑） */
    val isPort: Boolean get() = this == EDITORIAL || this == PAGES || this == GLASS || this == NOCTURNE

    companion object {
        fun fromRaw(raw: String): DockStyle = when (raw) {
            "v1" -> V1; "v5" -> V5; "v6" -> V6; "v7" -> V7; "v8" -> V8
            "v9" -> V9; "v10" -> V10; "v11" -> V11
            "editorial" -> EDITORIAL; "pages" -> PAGES
            "glass" -> GLASS; "nocturne" -> NOCTURNE
            else -> D0
        }
    }
}

/** V5 是否停靠在右侧（定制 → 底部导航栏 → 胶囊停靠）。 */
val sideDockOnRight: Boolean get() = CustomizationStore.current.dockSide == 1

private val rainbowColors = listOf(
    Color(0xFF6C5CE7), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFF59E0B),
)

/**
 * 底栏统一尺寸（设计稿 .dock 的默认规格）。
 *
 * 13 种样式共用同一组尺寸：项宽 78dp、项高 60dp、间距 4dp、内衬 8dp。
 * 之前每种样式各自决定宽度（pages 撑满 440dp、nocturne 撑满全宽、editorial 通栏、
 * v11 60dp 项宽…），切换样式时底栏的长宽会跟着变，看起来像"换了一套组件"。
 * 现在样式只决定「长什么样」，不决定「有多大」。
 */
private object DockMetrics {
    val itemW = 78.dp
    val itemH = 60.dp
    val gap = 4.dp
    val pad = 8.dp
}

/**
 * 主题化悬浮抽屉胶囊底栏（设计稿 .dock 全样式实现）：
 * d0 原有胶囊 / v1 展开药丸 / v5 侧边抽屉 / v6 流光光标 / v7 描边霓虹 /
 * v8 方块卡片 / v9 渐变实底 / v10 顶指示条 / v11 浮起圆片。
 * 支持沉浸浅感 / 七彩图标 / 不透明度 / 圆角（CustomizationStore）。
 */
@Composable
fun ThemeDockBar(
    theme: DockTheme,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainState = LocalMainPagerState.current
    val items = BottomBarDestination.entries.toList()
    val custom = CustomizationStore.current
    val style = DockStyle.fromRaw(custom.dockStyle)
    val immersive = custom.bottomBarImmersive
    val rainbow = custom.navRainbow
    val alphaK = custom.bottomBarAlpha.coerceIn(0.2f, 1f)
    val dockRadius = custom.bottomBarRadius.coerceIn(12f, 40f).dp

    val dark = me.weishu.kernelsu.ui.screen.ag.Ag.isDark
    val accent = me.weishu.kernelsu.ui.screen.ag.Ag.accent

    // ── 图标造型（设计稿 data-ic：几何形状，作用于 Dock 项形状与选中态）──
    val iconStyle = custom.iconStyle
    val gradFill = iconStyle == 103               // 七彩：选中项渐变实底 + 白图标
    val iconShapeOverride = when (iconStyle) {
        101 -> CircleShape                         // 正圆
        102 -> RoundedCornerShape(22.dp)           // 超椭圆
        else -> null                               // 圆角方 = 默认 19dp
    }

    val effBg = when {
        // 静态磨砂：没有实时采样，太透会直接看见底下滚动的内容，所以沉浸态也保持 86%
        immersive -> theme.bg.copy(alpha = theme.bg.alpha * 0.86f * alphaK)
        else -> theme.bg.copy(alpha = theme.bg.alpha * alphaK)
    }
    val effBorder = if (immersive) Color.Transparent else theme.border

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = if (navInset != 0.dp) 10.dp + navInset else 22.dp

    val itemW = DockMetrics.itemW
    val itemH = DockMetrics.itemH
    val gap = DockMetrics.gap
    val pad = DockMetrics.pad
    val shape = RoundedCornerShape(dockRadius)

    val indicatorX by animateDpAsState(
        targetValue = (itemW + gap) * mainState.selectedPage,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "dock-indicator",
    )

    // ── V5：侧边抽屉（左右停靠由「胶囊停靠」决定）──
    if (style == DockStyle.V5) {
        val onRight = sideDockOnRight
        val sideShape = RoundedCornerShape(dockRadius)
        // 设计稿 `.phone.side .dock{background:rgba(255,255,255,.86)}`：
        // 侧边胶囊比横向底栏更实一档，避免竖排叠在彩色卡片上时文字发糊。
        val sideBase = if (dark) 0.86f else 0.86f * alphaK
        val sideBg = theme.bg.copy(alpha = sideBase.coerceIn(0.2f, 1f))
        Box(
            modifier = modifier
                .padding(start = if (onRight) 0.dp else 14.dp, end = if (onRight) 14.dp else 0.dp)
                .shadow(1.dp, sideShape, spotColor = theme.shadow, ambientColor = theme.shadow)
                .clip(sideShape)
                .border(1.dp, effBorder, sideShape),
        ) {
            // 设计稿 .phone.side .dock 同样是 backdrop-filter blur(20px)
            DockFrost(sideShape, sideBg)
            Column(
                modifier = Modifier.padding(horizontal = pad, vertical = pad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                items.forEachIndexed { index, item ->
                    val selected = mainState.selectedPage == index
                    // 侧边胶囊：暗色模式下胶囊是深色玻璃，未选中图标用亮白才看得清；
                    // 选中项永远是渐变实底，图标固定纯白（设计稿 .phone.side .dit.on{color:#fff}）。
                    val tint = when {
                        selected -> Color.White
                        rainbow -> rainbowColors[index % 4]
                        dark -> Color(0xFFF2F1FA)
                        else -> theme.unselected
                    }
                    val itemShape = iconShapeOverride ?: RoundedCornerShape(22.dp)
                    Column(
                        modifier = Modifier
                            .width(itemW)
                            .height(itemH)
                            .then(
                                if (selected) Modifier.shadow(
                                    8.dp, itemShape,
                                    spotColor = accent.primary.copy(alpha = 0.4f),
                                    ambientColor = accent.primary.copy(alpha = 0.10f),
                                ) else Modifier
                            )
                            .clip(itemShape)
                            .background(
                                if (selected) Brush.linearGradient(
                                    listOf(accent.primary, accent.mid, accent.secondary)
                                ) else SolidColor(Color.Transparent)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { mainState.animateToPage(index) }
                            .padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        DockIcon(
                            item, index, selected, navigationBadge, rainbow,
                            tint = tint,
                            badgeTintOverride = if (selected) Color.White else null,
                        )
                        Text(
                            stringResource(item.label),
                            fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold,
                            color = tint, maxLines = 1,
                        )
                    }
                }
            }
        }
        return
    }

    // ── 横向胶囊：所有样式共用一个外壳（同宽同高），样式只决定内部装饰 ──
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPad)
                .shadow(
                    // 对齐 Miuix FloatingNavigationBarDefaults.ShadowElevation = 1dp。
                    // 之前这里用 14dp，在 Android 的 spot shadow 下会在胶囊下方投出一大片中性灰，
                    // 浅色页面上看着像"底栏底下压了一块黑"——设计稿的胶囊几乎是平的。
                    if (immersive) 0.dp else 1.dp,
                    shape,
                    spotColor = theme.shadow,
                    ambientColor = theme.shadow,
                )
                .clip(shape)
                .border(1.dp, effBorder, shape),
        ) {
            DockFrost(shape, effBg, gloss = theme.glass || style == DockStyle.GLASS)
            Box(Modifier.padding(pad)) {
                // 滑动选中指示底衬（d0 / v8）：位置与项宽严格对齐
                if (style == DockStyle.D0 || style == DockStyle.V8) {
                    Box(
                        Modifier
                            .offset(x = indicatorX)
                            .width(itemW)
                            .height(itemH)
                            .clip(
                                iconShapeOverride
                                    ?: if (style == DockStyle.V8) RoundedCornerShape(14.dp)
                                    else RoundedCornerShape(22.dp)
                            )
                            .background(
                                SolidColor(
                                    when {
                                        style != DockStyle.V8 -> theme.selectedBg
                                        dark -> Color(0xFF2A2940)
                                        else -> Color.White
                                    }
                                )
                            ),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    items.forEachIndexed { index, item ->
                        DockItem(
                            style = style,
                            item = item,
                            index = index,
                            selected = mainState.selectedPage == index,
                            navigationBadge = navigationBadge,
                            rainbow = rainbow,
                            theme = theme,
                            dark = dark,
                            gradFill = gradFill,
                            iconShapeOverride = iconShapeOverride,
                            onClick = { mainState.animateToPage(index) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个 Dock 项：尺寸固定 78×60（与默认 d0 一致），样式只改变底衬/描边/指示器与文字。
 */
@Composable
private fun DockItem(
    style: DockStyle,
    item: BottomBarDestination,
    index: Int,
    selected: Boolean,
    navigationBadge: NavigationBadgeState,
    rainbow: Boolean,
    theme: DockTheme,
    dark: Boolean,
    gradFill: Boolean,
    iconShapeOverride: androidx.compose.ui.graphics.Shape?,
    onClick: () -> Unit,
) {
    val accent = me.weishu.kernelsu.ui.screen.ag.Ag.accent
    val itemW = DockMetrics.itemW
    val itemH = DockMetrics.itemH

    val baseTint = if (rainbow && !selected) rainbowColors[index % 4] else theme.unselected

    val itemShape = when {
        iconShapeOverride != null -> iconShapeOverride
        style == DockStyle.V9 -> RoundedCornerShape(18.dp)
        style == DockStyle.V8 -> RoundedCornerShape(14.dp)
        style == DockStyle.V11 -> CircleShape
        else -> RoundedCornerShape(19.dp)
    }

    val gradient = Brush.linearGradient(listOf(accent.primary, accent.mid, accent.secondary))

    val itemBg: Brush? = when {
        style == DockStyle.V7 && selected -> null            // 描边霓虹：透明底
        style == DockStyle.V10 && selected -> null           // 顶指示条：透明底
        style == DockStyle.V8 && selected -> null            // 方块卡片：交给滑动白卡
        style == DockStyle.V11 && selected -> null           // 浮起圆片：单独画圆
        style == DockStyle.D0 && selected -> null            // 原有胶囊：交给滑动底衬
        (gradFill || style == DockStyle.V9) && selected -> gradient
        style == DockStyle.V1 && selected -> SolidColor(theme.selectedBg)
        style == DockStyle.V6 && selected -> Brush.radialGradient(
            listOf(accent.primary.copy(alpha = 0.32f), accent.secondary.copy(alpha = 0.14f), Color.Transparent),
        )
        style == DockStyle.PAGES && selected -> SolidColor(theme.selectedBg)
        style == DockStyle.GLASS && selected -> SolidColor(theme.selectedBg)
        style == DockStyle.NOCTURNE && selected -> SolidColor(theme.selectedBg)
        else -> null
    }

    val fgTint = when {
        (gradFill || style == DockStyle.V9) && selected -> Color.White
        style == DockStyle.EDITORIAL -> if (selected) theme.selected else baseTint
        PAGES_GLASS_NOCTURNE.contains(style) && selected -> theme.selected
        selected -> theme.selected
        else -> baseTint
    }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "dock-icon-$index",
    )
    val lift by animateDpAsState(
        targetValue = if (style == DockStyle.EDITORIAL && selected) (-2).dp else 0.dp,
        animationSpec = tween(260),
        label = "dock-lift-$index",
    )
    val underline by animateDpAsState(
        targetValue = if (style == DockStyle.EDITORIAL && selected) 18.dp else 0.dp,
        animationSpec = tween(280),
        label = "dock-underline-$index",
    )

    Box(
        modifier = Modifier
            .width(itemW)
            .height(itemH)
            // 不整体 clip：v10 顶部指示条需要溢出到项外
            .then(if (itemBg != null) Modifier.background(itemBg, itemShape) else Modifier)
            .then(
                when {
                    style == DockStyle.V7 && selected -> Modifier
                        .border(1.6.dp, accent.primary.copy(alpha = 0.45f), itemShape)
                        .drawBehind {
                            // 外光环 box-shadow:0 0 0 4px rgba(accent,.08)
                            val p = 4.dp.toPx()
                            drawRoundRect(
                                color = accent.primary.copy(alpha = 0.08f),
                                topLeft = androidx.compose.ui.geometry.Offset(-p, -p),
                                size = androidx.compose.ui.geometry.Size(size.width + p * 2, size.height + p * 2),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(19.dp.toPx() + p),
                            )
                        }
                    style == DockStyle.V10 && selected -> Modifier
                        .border(1.2.dp, accent.primary.copy(alpha = 0.20f), itemShape)
                    else -> Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // V11 浮起圆片：圆片比项本身小一圈，居中
        if (style == DockStyle.V11 && selected) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(gradient),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = lift),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DockIcon(
                item, index, selected, navigationBadge, rainbow,
                tint = fgTint,
                badgeTintOverride = if (fgTint == Color.White) Color.White else null,
                scale = iconScale,
                glow = style == DockStyle.V6 && selected,
                big = (gradFill || style == DockStyle.V9) && selected,
            )
            // 标签：d0 / v1(选中) / v5 / pages(选中) / glass / nocturne 常显样式
            if (showLabel(style, selected)) {
                Text(
                    text = stringResource(item.label),
                    fontSize = if (style == DockStyle.GLASS) 7.5.sp else 10.sp,
                    lineHeight = if (style == DockStyle.GLASS) 9.sp else 12.sp,
                    maxLines = 1,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = fgTint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Editorial：选中项下方短横线
            if (style == DockStyle.EDITORIAL) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .width(underline)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(if (selected) theme.selected else Color.Transparent),
                )
            }
        }

        // V10 顶指示条 + 中心小圆点
        if (style == DockStyle.V10 && selected) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-5).dp)
                    .width(22.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.linearGradient(listOf(accent.primary, accent.secondary))),
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 8.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent.primary, accent.secondary))),
            )
        }
        // GLASS 顶部霓虹条（悬在药丸内衬外）
        if (style == DockStyle.GLASS && selected) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .size(width = 22.dp, height = 3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary))),
            )
        }
        // NOCTURNE 顶部指示条
        if (style == DockStyle.NOCTURNE && selected) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-2).dp)
                    .size(width = 24.dp, height = 2.5.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(accent.primary, accent.secondary))),
            )
        }
    }
}

private val PAGES_GLASS_NOCTURNE = setOf(DockStyle.PAGES, DockStyle.GLASS, DockStyle.NOCTURNE)

private fun showLabel(style: DockStyle, selected: Boolean): Boolean = when (style) {
    DockStyle.D0, DockStyle.GLASS, DockStyle.NOCTURNE -> true
    DockStyle.V1, DockStyle.PAGES -> selected
    else -> false
}

/**
 * 设计稿 .dock 的毛玻璃：`backdrop-filter: blur(20px) saturate(1.4)` + `rgba(255,255,255,.78)`。
 *
 * 实现上用「静态磨砂」而不是实时背景采样：底栏在每一页的首帧就要绘制，实时模糊需要先把
 * 整页内容录进 RenderNode 再回读采样，部分真机 GPU 在首帧直接崩溃（启动页卡住→闪退）。
 * 设计稿底色本来就是 78% 白，静态版叠上半透明底 + 顶部高光 + 底部内阴影后观感几乎一致，
 * 而且滚动时不会有采样延迟导致的"糊穿"。
 */
@Composable
private fun BoxScope.DockFrost(
    shape: androidx.compose.ui.graphics.Shape,
    tint: Color,
    gloss: Boolean = false,
) {
    // 玻璃本体：半透明底 + 上亮下暗的厚度感。
    // 暗色下顶部高光必须压低：胶囊底是 #1C1B2E 的深玻璃，19% 白叠上去就是
    // 一条横贯胶囊顶部的白雾（夜间模式里"胶囊上方那块白"就是它）。
    val dark = me.weishu.kernelsu.ui.screen.ag.Ag.isDark
    val topLight = if (dark) Color(0x08FFFFFF) else Color(0x30FFFFFF)
    Box(
        Modifier
            .matchParentSize()
            .clip(shape)
            .background(tint)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        // 顶部一层薄高光给玻璃"厚度"，底部不再压黑。
                        0f to topLight,
                        0.55f to Color(0x00FFFFFF),
                        1f to Color(0x00FFFFFF),
                        startY = 0f,
                        endY = size.height,
                    ),
                )
            },
    )
    if (gloss) {
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(if (dark) Color(0x1AFFFFFF) else Color(0x73FFFFFF), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.32f,
                        ),
                    )
                },
        )
    }
}

/** Dock 单项图标 + 徽标 */
@Composable
private fun DockIcon(
    item: BottomBarDestination,
    index: Int,
    selected: Boolean,
    navigationBadge: NavigationBadgeState,
    rainbow: Boolean,
    tint: Color,
    badgeTintOverride: Color?,
    scale: Float = 1f,
    glow: Boolean = false,
    big: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        // 设计稿 .dock .dit 图标 22px（选中态 24px），项高 60dp
        val iconSize = if (big) 24.dp else 22.dp
        Icon(
            imageVector = item.filledIcon,
            contentDescription = stringResource(item.label),
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    if (selected && !big) translationY = -1.dp.toPx()
                },
        )
        val badge = navigationBadgeFor(index, navigationBadge, floating = true)
        if (badge != null) {
            // 设计稿 .dit .bdg：17px 品红圆角徽标 + 2px 白边，位于项右上
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = (-2).dp)
            ) {
                Box(
                    Modifier
                        .height(17.dp)
                        .defaultMinSize(minWidth = 17.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(me.weishu.kernelsu.ui.screen.ag.Ag.accent.secondary)
                        .border(
                            2.dp,
                            if (me.weishu.kernelsu.ui.screen.ag.Ag.isDark) Color(0xFF1C1B2E) else Color.White,
                            RoundedCornerShape(9.dp),
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        badgeTextFor(index, navigationBadge) ?: "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 各皮肤各自的专属抽屉风格（跟随皮肤自身的明暗与设计语言） */
object AgDockThemes {

    /** FoxSu Redesign：晨曦玻璃，明暗双态（强调色跟随全局配色） */
    @Composable
    fun redesign(): DockTheme {
        val dark = me.weishu.kernelsu.ui.screen.ag.Ag.isDark
        val ac = me.weishu.kernelsu.ui.screen.ag.Ag.accent
        return if (dark) DockTheme(
            bg = Color(0xCC1C1B2E), border = Color(0x1FFFFFFF), shadow = Color(0x66000000),
            selected = ac.primary, unselected = Color(0xFF8C89A6),
            selectedBg = ac.softDark, badge = ac.secondary, glass = false,
            backdrop = Color(0xFF1A192E),
        ) else DockTheme(
            // 品牌稿（foxsu-brand-ui）：胶囊是接近实心的白，不再靠通透感取巧；
            // 选中块取淡同色相药丸，未选中图标/文字用设计稿的灰紫 #8E8AA3。
            bg = Color(0xF2FFFFFF), border = Color(0x66FFFFFF), shadow = Color(0x2432286E),
            selected = ac.primary, unselected = Color(0xFF8E8AA3),
            selectedBg = ac.soft, badge = ac.secondary, glass = false,
            backdrop = Color(0xFFF2F4FA),
        )
    }

    /** Pages：极简留白（亮）→ 墨面（暗），单一强调点 */
    @Composable
    fun fresh(): DockTheme {
        val dark = me.weishu.kernelsu.ui.screen.ag.Ag.isDark
        val ac = me.weishu.kernelsu.ui.screen.ag.Ag.accent
        return if (dark) DockTheme(
            bg = Color(0xF219191F), border = Color(0x29FFFFFF), shadow = Color(0x66000000),
            selected = ac.primary, unselected = Color(0xFF8A8F9C),
            selectedBg = Color(0x1FFFFFFF), badge = ac.primary,
            backdrop = Color(0xFF141419),
        ) else DockTheme(
            bg = Color(0xF6FFFFFF), border = Color(0x14111111), shadow = Color(0x1A111111),
            selected = ac.primary, unselected = Color(0xFF9AA0B0),
            selectedBg = ac.soft, badge = ac.primary,
            backdrop = Color(0xFFFAFAF7),
        )
    }

    /** Editorial：纸面质感 + 墨描边 + 强调色 */
    val editorial: DockTheme
        @Composable get() = DockTheme(
            bg = Color(0xFFFFFCF6), border = Color(0x471D1C22), shadow = Color(0x33282418),
            selected = me.weishu.kernelsu.ui.screen.ag.Ag.accent.primary, unselected = Color(0xFF9C968B),
            selectedBg = Color(0x141D1C22), badge = me.weishu.kernelsu.ui.screen.ag.Ag.accent.secondary,
            backdrop = Color(0xFFFFFCF6),
        )

    /** Daylight：白玻璃（亮）→ 炭玻璃（暗） */
    @Composable
    fun glass(): DockTheme {
        val dark = me.weishu.kernelsu.ui.theme.glassTokens().isDark
        val ac = me.weishu.kernelsu.ui.screen.ag.Ag.accent
        return if (dark) DockTheme(
            bg = Color(0xE614141E), border = Color(0x2EFFFFFF), shadow = Color(0x66000000),
            selected = ac.primary, unselected = Color(0xFF7C8294),
            selectedBg = ac.softDark, badge = ac.secondary, glass = true,
            backdrop = Color(0xFF0F0F16),
        ) else DockTheme(
            bg = Color(0xCCFFFFFF), border = Color(0x33FFFFFF), shadow = Color(0x2E32286E),
            selected = ac.primary, unselected = Color(0xFF9AA0B0),
            selectedBg = ac.soft, badge = ac.secondary, glass = true,
            backdrop = Color(0xFFF7F6FB),
        )
    }

    /** Nocturne：OLED 纯黑胶囊 + 极光（恒暗） */
    val nocturne: DockTheme
        @Composable get() = DockTheme(
            bg = Color(0xF2101218), border = Color(0x26FFFFFF), shadow = Color(0x66000000),
            selected = me.weishu.kernelsu.ui.screen.ag.Ag.accent.primary, unselected = Color(0xFF5D6376),
            selectedBg = me.weishu.kernelsu.ui.screen.ag.Ag.accent.softDark,
            badge = me.weishu.kernelsu.ui.screen.ag.Ag.accent.secondary, glass = true,
            backdrop = Color(0xFF0B0A14),
        )
}

/** 取导航角标数值（设计稿 .bdg 显示纯数字） */
private fun badgeTextFor(index: Int, state: NavigationBadgeState): String? {
    val badge = badgeFor(index, state) ?: return null
    return badge.count.toString()
}
