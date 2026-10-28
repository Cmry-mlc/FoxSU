package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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

/** 底栏样式（设计稿 9 种） */
enum class DockStyle { D0, V1, V5, V6, V7, V8, V9, V10, V11;

    companion object {
        fun fromRaw(raw: String): DockStyle = when (raw) {
            "v1" -> V1; "v5" -> V5; "v6" -> V6; "v7" -> V7; "v8" -> V8
            "v9" -> V9; "v10" -> V10; "v11" -> V11; else -> D0
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

    // ── 图标风格（设计稿 data-ic / data-fox，作用于 Dock 项形状与选中态）──
    val iconStyle = custom.iconStyle
    val foxRes: Int? = when (iconStyle) {
        0 -> me.weishu.kernelsu.R.drawable.fox_icon_cute
        2 -> me.weishu.kernelsu.R.drawable.fox_icon_wave
        3 -> me.weishu.kernelsu.R.drawable.fox_icon_glance
        4 -> me.weishu.kernelsu.R.drawable.fox_icon_joy
        else -> null
    }
    val gradFill = iconStyle == 103               // 七彩：选中项渐变实底 + 白图标
    val iconShapeOverride = when (iconStyle) {
        101 -> CircleShape                         // 正圆
        102 -> RoundedCornerShape(22.dp)           // 超椭圆
        else -> null                               // 圆角方 = 默认 19dp
    }

    val effBg = when {
        immersive -> theme.bg.copy(alpha = theme.bg.alpha * 0.58f * alphaK)
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

    // ── V5：侧边抽屉（左侧垂直居中）──
    if (style == DockStyle.V5) {
        Column(
            modifier = modifier
                .padding(start = 14.dp)
                .clip(RoundedCornerShape(30.dp))
                // 设计稿 .phone.side .dock：白 86%
                .background(if (dark) theme.bg else theme.bg.copy(alpha = 0.86f * alphaK))
                .border(1.dp, effBorder, RoundedCornerShape(30.dp))
                .shadow(14.dp, RoundedCornerShape(30.dp), spotColor = theme.shadow, ambientColor = theme.shadow)
                .padding(10.dp, 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = mainState.selectedPage == index
                val tint = if (rainbow && !selected) rainbowColors[index % 4] else theme.unselected
                Column(
                    modifier = Modifier
                        .width(62.dp)
                        .height(60.dp)
                        .clip(iconShapeOverride ?: RoundedCornerShape(22.dp))
                        .background(
                            if (selected) Brush.linearGradient(
                                listOf(Color(0xFF6C5CE7), Color(0xFFEC4899))
                            ) else androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { mainState.animateToPage(index) }
                        .padding(9.dp, 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (foxRes != null && selected) {
                        // 狐狸图标风格：选中项覆盖为狐狸图
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(foxRes),
                            contentDescription = stringResource(item.label),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        DockIcon(item, index, selected, navigationBadge, rainbow, tint, badgeTintOverride = Color.White)
                        Text(
                            stringResource(item.label),
                            fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (selected) Color.White else tint, maxLines = 1,
                        )
                    }
                }
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPad)
                .shadow(if (immersive) 8.dp else 14.dp, RoundedCornerShape(dockRadius), spotColor = theme.shadow, ambientColor = theme.shadow)
                .clip(RoundedCornerShape(dockRadius))
                .background(effBg)
                .border(1.dp, effBorder, RoundedCornerShape(dockRadius))
                .drawBehind {
                    if (theme.glass) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0x59FFFFFF), Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.25f,
                            ),
                        )
                    }
                }
                .padding(8.dp),
        ) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                items.forEachIndexed { index, item ->
                    val selected = mainState.selectedPage == index
                    val baseTint = if (rainbow && !selected) rainbowColors[index % 4] else theme.unselected

                    val itemBg: androidx.compose.ui.graphics.Brush? = when {
                        gradFill && selected -> Brush.linearGradient(
                            listOf(Color(0xFF6C5CE7), Color(0xFFA855F7), Color(0xFFEC4899))
                        )
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

                    Column(
                        modifier = Modifier
                            .width(itemW)
                            .height(55.dp)
                            .clip(itemShape)
                            .then(
                                when {
                                    style == DockStyle.V7 && selected -> Modifier
                                        .border(1.6.dp, Color(0x736C5CE7), itemShape)
                                        .background(Color(0x146C5CE7))
                                    style == DockStyle.V8 && selected -> Modifier
                                        .background(if (dark) Color(0xFF2A2940) else Color.White)
                                        .border(1.dp, Color(0x121E1946), itemShape)
                                    itemBg != null -> Modifier.background(itemBg)
                                    else -> Modifier
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { mainState.animateToPage(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (foxRes != null && selected) {
                            // 狐狸图标风格：选中项整体覆盖为狐狸图（设计稿 data-fox ::after）
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(foxRes),
                                contentDescription = stringResource(item.label),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(55.dp),
                            )
                        } else {
                            // V10 顶指示条
                            if (style == DockStyle.V10 && selected) {
                                Box(
                                    Modifier
                                        .padding(bottom = 3.dp)
                                        .width(22.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFFEC4899)))),
                                )
                            }
                            DockIcon(
                                item, index, selected, navigationBadge, rainbow,
                                tint = fgTint,
                                badgeTintOverride = if (fgTint == Color.White) Color.White else null,
                                scale = iconScale,
                                glow = style == DockStyle.V6 && selected,
                                big = (gradFill || style == DockStyle.V9 || style == DockStyle.V11) && selected,
                            )
                            // d0 / v1 显示文字
                            if (style == DockStyle.D0 || style == DockStyle.V1) {
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
                    }
                }
            }
        }
    }
}

/** Dock 单项图标 + 徽标 */
@Composable
private fun ColumnScope.DockIcon(
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
) {
    Box(contentAlignment = Alignment.TopCenter) {
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
            Box(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)) { badge() }
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
            bg = Color(0xCC1C1B2E), border = Color(0x14FFFFFF), shadow = Color(0x66000000),
            selected = Color(0xFF9C90FF), unselected = Color(0xFF7C7A98),
            selectedBg = Color(0x2E6C5CE7), badge = Color(0xFFEC4899), glass = false,
            backdrop = Color(0xFF1A192E),
        ) else DockTheme(
            bg = Color(0xC7FFFFFF), border = Color(0x99FFFFFF), shadow = Color(0x2E32286E),
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
