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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.data.settings.CustomizationStore

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

private val rainbowColors = listOf(
    Color(0xFF6C5CE7), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFF59E0B),
)

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

    val itemW = 76.dp
    val gap = 4.dp
    val indicatorX by animateDpAsState(
        targetValue = itemW * mainState.selectedPage + gap * mainState.selectedPage,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "dock-indicator",
    )

    // ── V5：侧边抽屉（左侧垂直居中，设计稿 .phone.side .dock）──
    if (style == DockStyle.V5) {
        val sideShape = RoundedCornerShape(30.dp)
        // 侧边胶囊是竖排叠在卡片上的，太透会透出卡片颜色、文字发糊，
        // 所以比横向底栏更实一些（0.94），保持「一块独立玻璃浮在内容上」的观感。
        val sideBg = theme.bg.copy(
            alpha = if (dark) 0.94f else (0.94f * alphaK).coerceAtMost(1f),
        )
        Box(
            modifier = modifier
                .padding(start = 14.dp)
                .shadow(14.dp, sideShape, spotColor = theme.shadow, ambientColor = theme.shadow)
                .clip(sideShape)
                .border(1.dp, effBorder, sideShape),
        ) {
            // 设计稿 .phone.side .dock 同样是 backdrop-filter blur(20px)
            DockFrost(sideShape, sideBg)
            Column(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                val shape = iconShapeOverride ?: RoundedCornerShape(22.dp)
                Column(
                    modifier = Modifier
                        .width(62.dp)
                        .height(60.dp)
                        .then(
                            if (selected) Modifier.shadow(
                                8.dp, shape,
                                spotColor = Color(0x666C5CE7),
                                ambientColor = Color(0x1A6C5CE7),
                            ) else Modifier
                        )
                        .clip(shape)
                        .background(
                            if (selected) Brush.linearGradient(
                                listOf(Color(0xFF6C5CE7), Color(0xFFA855F7), Color(0xFFEC4899))
                            ) else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
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

    // ── EDITORIAL：复刻 Material「纸面」底栏 —— 全宽贴底 + 1dp 墨线 + 选中上浮/短横线 ──
    if (style == DockStyle.EDITORIAL) {
        Column(
            modifier
                .fillMaxWidth()
                .padding(bottom = navInset)
                .background(theme.bg),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(theme.border))
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items.forEachIndexed { index, item ->
                    val selected = mainState.selectedPage == index
                    val lift by animateDpAsState(
                        if (selected) (-2).dp else 0.dp, tween(260), label = "ed-lift-$index",
                    )
                    val scale by animateFloatAsState(
                        if (selected) 1.08f else 1f, tween(260), label = "ed-scale-$index",
                    )
                    val lineW by animateDpAsState(
                        if (selected) 18.dp else 0.dp, tween(280), label = "ed-line-$index",
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { mainState.animateToPage(index) }
                            .padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        DockIcon(
                            item, index, selected, navigationBadge, rainbow,
                            tint = if (rainbow && !selected) rainbowColors[index % 4]
                            else if (selected) theme.selected else theme.unselected,
                            badgeTintOverride = null,
                            scale = scale,
                            modifier = Modifier.offset(y = lift),
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .width(lineW)
                                .height(2.dp)
                                .clip(CircleShape)
                                .background(if (selected) theme.selected else Color.Transparent),
                        )
                    }
                }
            }
        }
        return
    }

    // ── PAGES：复刻 Fresh「白色药丸」—— 等分项，选中项展开文字 ──
    if (style == DockStyle.PAGES) {
        val pill = RoundedCornerShape(percent = 50)
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .padding(bottom = bottomPad)
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .shadow(14.dp, pill, spotColor = theme.shadow, ambientColor = theme.shadow)
                    .clip(pill)
                    .border(1.dp, theme.border, pill),
            ) {
                DockFrost(pill, effBg)
                Row(
                    Modifier.fillMaxWidth().height(58.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                items.forEachIndexed { index, item ->
                    val selected = mainState.selectedPage == index
                    val tint = if (rainbow && !selected) rainbowColors[index % 4]
                    else if (selected) theme.selected else theme.unselected
                    Row(
                        Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { mainState.animateToPage(index) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DockIcon(
                            item, index, selected, navigationBadge, rainbow,
                            tint = tint, badgeTintOverride = null,
                            scale = if (selected) 1.06f else 1f,
                        )
                        if (selected) {
                            Text(
                                stringResource(item.label),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = tint, maxLines = 1,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
                }
            }
        }
        return
    }

    // ── GLASS：复刻 Daylight「液态药丸」—— 4dp 内衬 + 选中药丸底 + 顶部霓虹条 ──
    if (style == DockStyle.GLASS) {
        val pill = RoundedCornerShape(99.dp)
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .padding(bottom = bottomPad)
                    .shadow(14.dp, pill, spotColor = theme.shadow, ambientColor = theme.shadow)
                    .clip(pill)
                    .border(1.dp, theme.border, pill),
            ) {
                DockFrost(pill, effBg)
                Row(
                    Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                items.forEachIndexed { index, item ->
                    val selected = mainState.selectedPage == index
                    val tint = if (rainbow && !selected) rainbowColors[index % 4]
                    else if (selected) theme.selected else theme.unselected
                    Box(
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { mainState.animateToPage(index) },
                    ) {
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (selected) theme.selectedBg else Color.Transparent)
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            DockIcon(
                                item, index, selected, navigationBadge, rainbow,
                                tint = tint, badgeTintOverride = null,
                                scale = if (selected) 1.06f else 1f,
                            )
                            Text(
                                stringResource(item.label),
                                fontSize = 7.5.sp, lineHeight = 9.sp,
                                color = tint, maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        // .ditem.on::before —— 顶部 22×3 霓虹条，悬在药丸外 4dp
                        if (selected) {
                            Box(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-4).dp)
                                    .size(width = 22.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF6C5CE7), Color(0xFFEC4899))
                                        )
                                    ),
                            )
                        }
                    }
                }
                }
            }
        }
        return
    }

    // ── NOCTURNE：复刻 Nocturne「暗夜霓虹」—— 64dp 高、22dp 圆角、顶部 24×2.5 指示条 ──
    if (style == DockStyle.NOCTURNE) {
        val r = RoundedCornerShape(22.dp)
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .padding(start = 18.dp, end = 18.dp, bottom = (navInset.takeIf { it != 0.dp } ?: 16.dp))
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(r)
                    .border(1.dp, theme.border, r),
            ) {
                DockFrost(r, effBg)
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                items.forEachIndexed { index, item ->
                    val selected = mainState.selectedPage == index
                    val tint = if (rainbow && !selected) rainbowColors[index % 4]
                    else if (selected) theme.selected else theme.unselected
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { mainState.animateToPage(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = 6.dp)
                                    .size(width = 24.dp, height = 2.5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF6C5CE7), Color(0xFFEC4899))
                                        )
                                    ),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.offset(y = if (selected) (-1).dp else 0.dp),
                        ) {
                            DockIcon(
                                item, index, selected, navigationBadge, rainbow,
                                tint = tint, badgeTintOverride = null,
                                scale = if (selected) 1.04f else 1f,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                stringResource(item.label),
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = tint, maxLines = 1,
                            )
                        }
                    }
                }
                }
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        // 设计稿 .dock：backdrop-filter blur(20px) saturate(1.4) + rgba(255,255,255,.78)
        val shellShape = RoundedCornerShape(if (style == DockStyle.V8) 20.dp else dockRadius)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPad)
                .shadow(if (immersive) 8.dp else 14.dp, shellShape, spotColor = theme.shadow, ambientColor = theme.shadow)
                .clip(shellShape)
                .border(1.dp, effBorder, shellShape),
        ) {
            DockFrost(shellShape, effBg, gloss = theme.glass)
            Box(Modifier.padding(if (style == DockStyle.V8) 6.dp else 8.dp)) {
            // 滑动选中指示底衬（d0 / v8）
            if (style == DockStyle.D0 || style == DockStyle.V8) {
                Box(
                    Modifier
                        .offset(x = indicatorX)
                        .width(itemW)
                        .height(55.dp)
                        .clip(
                            iconShapeOverride
                                ?: if (style == DockStyle.V8) RoundedCornerShape(14.dp) else RoundedCornerShape(19.dp)
                        )
                        .background(
                            androidx.compose.ui.graphics.SolidColor(
                                if (style == DockStyle.V8) {
                                    if (dark) Color(0xFF2A2940) else Color.White
                                } else {
                                    theme.selectedBg
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
                    val selected = mainState.selectedPage == index
                    val baseTint = if (rainbow && !selected) rainbowColors[index % 4] else theme.unselected

                    // V1 展开药丸：未选中只剩图标（46dp），选中扩成 76dp 并展开文字
                    val expanded = style == DockStyle.V1 && selected
                    val targetWidth = when {
                        style == DockStyle.V1 -> if (expanded) 76.dp else 46.dp
                        style == DockStyle.V11 -> 55.dp
                        else -> itemW
                    }
                    val itemWidth by animateDpAsState(
                        targetValue = targetWidth,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "dock-item-w-$index",
                    )

                    val itemBg: androidx.compose.ui.graphics.Brush? = when {
                        style == DockStyle.V7 && selected -> null            // 描边霓虹：透明底
                        style == DockStyle.V10 && selected -> null           // 顶指示条：透明底
                        style == DockStyle.V8 && selected -> null            // 方块卡片：交给滑动白卡
                        gradFill && selected -> Brush.linearGradient(
                            listOf(Color(0xFF6C5CE7), Color(0xFFA855F7), Color(0xFFEC4899))
                        )
                        style == DockStyle.V1 && selected ->
                            androidx.compose.ui.graphics.SolidColor(theme.selectedBg)
                        style == DockStyle.V6 && selected -> Brush.radialGradient(
                            listOf(Color(0x526C5CE7), Color(0x24EC4899), Color.Transparent),
                        )
                        style == DockStyle.V9 && selected -> Brush.linearGradient(
                            listOf(Color(0xFF6C5CE7), Color(0xFFA855F7), Color(0xFFEC4899))
                        )
                        style == DockStyle.V11 && selected -> Brush.linearGradient(
                            listOf(Color(0xFF6C5CE7), Color(0xFFEC4899))
                        )
                        else -> null
                    }

                    val fgTint = when {
                        (gradFill || style == DockStyle.V9 || style == DockStyle.V11) && selected -> Color.White
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

                    val itemShape = when {
                        iconShapeOverride != null -> iconShapeOverride
                        style == DockStyle.V9 -> RoundedCornerShape(18.dp)
                        style == DockStyle.V8 -> RoundedCornerShape(14.dp)
                        style == DockStyle.V11 -> CircleShape
                        else -> RoundedCornerShape(19.dp)
                    }

                    // V11 浮起圆片：设计稿 .dock.v11 .dit{width:55px;margin:0 10px}
                    Box(
                        Modifier.padding(horizontal = if (style == DockStyle.V11) 8.dp else 0.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .height(55.dp)
                                // 不整体 clip：v10 顶部指示条需要溢出到项外
                                .then(
                                    if (itemBg != null) Modifier.background(itemBg, itemShape) else Modifier
                                )
                                .then(
                                    when {
                                        style == DockStyle.V7 && selected -> Modifier
                                            .border(1.6.dp, Color(0x736C5CE7), itemShape)
                                            .drawBehind {
                                                // 外光环 box-shadow:0 0 0 4px rgba(108,92,231,.08)
                                                val pad = 4.dp.toPx()
                                                drawRoundRect(
                                                    color = Color(0x146C5CE7),
                                                    topLeft = androidx.compose.ui.geometry.Offset(-pad, -pad),
                                                    size = androidx.compose.ui.geometry.Size(size.width + pad * 2, size.height + pad * 2),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(19.dp.toPx() + pad),
                                                )
                                            }
                                        style == DockStyle.V10 && selected -> Modifier
                                            .border(1.2.dp, Color(0x336C5CE7), itemShape)
                                        else -> Modifier
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { mainState.animateToPage(index) },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                DockIcon(
                                    item, index, selected, navigationBadge, rainbow,
                                    tint = fgTint,
                                    badgeTintOverride = if (fgTint == Color.White) Color.White else null,
                                    scale = iconScale,
                                    glow = style == DockStyle.V6 && selected,
                                    big = (gradFill || style == DockStyle.V9 || style == DockStyle.V11) && selected,
                                )
                                // d0 常显文字；v1 只在选中项展开文字
                                if (style == DockStyle.D0 || expanded) {
                                    Text(
                                        text = stringResource(item.label),
                                        fontSize = 10.5.sp,
                                        lineHeight = 13.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Bold,
                                        color = fgTint,
                                    )
                                }
                            }
                            // V10 顶指示条 + 中心小圆点（设计稿 .dock.v10 .dit.on::before/::after）
                            if (style == DockStyle.V10 && selected) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-5).dp)
                                        .width(22.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFEC4899)))),
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = 8.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFEC4899)))),
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
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
    // 玻璃本体：半透明底 + 上亮下暗的厚度感
    Box(
        Modifier
            .matchParentSize()
            .clip(shape)
            .background(tint)
            .drawBehind {
                // 顶部高光 → 中部透明 → 底部极淡内阴影，模拟玻璃厚度
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color(0x38FFFFFF),
                        0.42f to Color(0x0AFFFFFF),
                        1f to Color(0x14000000),
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
                            listOf(Color(0x73FFFFFF), Color.Transparent),
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
        val iconSize = if (big) 26.dp else 22.dp
        Icon(
            imageVector = item.icon,
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
                        .background(Color(0xFFEC4899))
                        .border(2.dp, if (me.weishu.kernelsu.ui.screen.ag.Ag.isDark) Color(0xFF1C1B2E) else Color.White, RoundedCornerShape(9.dp))
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

    /** FoxSu Redesign：晨曦玻璃，明暗双态 */
    @Composable
    fun redesign(): DockTheme {
        val dark = me.weishu.kernelsu.ui.screen.ag.Ag.isDark
        return if (dark) DockTheme(
            bg = Color(0xB31C1B2E), border = Color(0x1FFFFFFF), shadow = Color(0x66000000),
            selected = Color(0xFF9C90FF), unselected = Color(0xFF7C7A98),
            selectedBg = Color(0x2E6C5CE7), badge = Color(0xFFEC4899), glass = false,
            backdrop = Color(0xFF1A192E),
        ) else DockTheme(
            bg = Color(0xA8FFFFFF), border = Color(0xB3FFFFFF), shadow = Color(0x2E32286E),
            selected = Color(0xFF6C5CE7), unselected = Color(0xFF66647A),
            selectedBg = Color(0x1A6C5CE7), badge = Color(0xFFEC4899), glass = false,
            backdrop = Color(0xFFF2F4FA),
        )
    }

    /** Pages：极简留白（亮）→ 墨面（暗），单一蓝点 */
    @Composable
    fun fresh(): DockTheme {
        val dark = me.weishu.kernelsu.ui.screen.ag.Ag.isDark
        return if (dark) DockTheme(
            bg = Color(0xF219191F), border = Color(0x29FFFFFF), shadow = Color(0x66000000),
            selected = Color(0xFF7DA9FF), unselected = Color(0xFF8A8F9C),
            selectedBg = Color(0x1F2D6BFF), badge = Color(0xFF2D6BFF),
            backdrop = Color(0xFF141419),
        ) else DockTheme(
            bg = Color(0xF6FFFFFF), border = Color(0x14111111), shadow = Color(0x1A111111),
            selected = Color(0xFF2D6BFF), unselected = Color(0xFF9AA0B0),
            selectedBg = Color(0x142D6BFF), badge = Color(0xFF2D6BFF),
            backdrop = Color(0xFFFAFAF7),
        )
    }

    /** Editorial：纸面质感 + 墨描边 + 信号橙（恒纸色） */
    val editorial = DockTheme(
        bg = Color(0xFFFFFCF6), border = Color(0x471D1C22), shadow = Color(0x33282418),
        selected = Color(0xFFE8502E), unselected = Color(0xFF9C968B),
        selectedBg = Color(0x1AE8502E), badge = Color(0xFFE8502E),
        backdrop = Color(0xFFFFFCF6),
    )

    /** Daylight：白玻璃（亮）→ 炭玻璃（暗），紫罗兰 + 薄荷点缀 */
    @Composable
    fun glass(): DockTheme {
        val dark = me.weishu.kernelsu.ui.theme.glassTokens().isDark
        return if (dark) DockTheme(
            bg = Color(0xE614141E), border = Color(0x2EFFFFFF), shadow = Color(0x66000000),
            selected = Color(0xFF9C90FF), unselected = Color(0xFF7C8294),
            selectedBg = Color(0x338B7BFF), badge = Color(0xFF4DE2C5), glass = true,
            backdrop = Color(0xFF0F0F16),
        ) else DockTheme(
            bg = Color(0xCCFFFFFF), border = Color(0x33FFFFFF), shadow = Color(0x2E32286E),
            selected = Color(0xFF6A57F5), unselected = Color(0xFF9AA0B0),
            selectedBg = Color(0x1A6A57F5), badge = Color(0xFF0FA98B), glass = true,
            backdrop = Color(0xFFF7F6FB),
        )
    }

    /** Nocturne：OLED 纯黑胶囊 + 紫罗兰极光（恒暗） */
    val nocturne = DockTheme(
        bg = Color(0xF2101218), border = Color(0x26FFFFFF), shadow = Color(0x66000000),
        selected = Color(0xFF8B7BFF), unselected = Color(0xFF5D6376),
        selectedBg = Color(0x2E8B7BFF), badge = Color(0xFF4DE2C5), glass = true,
        backdrop = Color(0xFF0B0A14),
    )
}

/** 取导航角标数值（设计稿 .bdg 显示纯数字） */
private fun badgeTextFor(index: Int, state: NavigationBadgeState): String? {
    val badge = badgeFor(index, state) ?: return null
    return badge.count.toString()
}
