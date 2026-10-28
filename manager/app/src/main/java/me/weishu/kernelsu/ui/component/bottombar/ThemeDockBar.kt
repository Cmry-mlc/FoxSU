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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.LocalMainPagerState
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ui.theme.glassTokens

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
)

/**
 * 主题化悬浮抽屉胶囊底栏（设计稿 .dock 的全皮肤实现）。
 * - 选中指示底衬在项之间平滑滑动（animateDp 位移）
 * - 沉浸光感：顶部高光 + 选中光晕 + 弹性缩放
 */
@Composable
fun ThemeDockBar(
    theme: DockTheme,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier = Modifier,
) {
    val mainState = LocalMainPagerState.current
    val items = BottomBarDestination.entries.toList()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = if (navInset != 0.dp) 10.dp + navInset else 30.dp

    val itemW = 76.dp
    val gap = 4.dp
    // 滑动指示器位移（相对 Row 内容起点）
    val indicatorX by animateDpAsState(
        targetValue = itemW * mainState.selectedPage + gap * mainState.selectedPage,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "dock-indicator",
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPad)
                .shadow(14.dp, RoundedCornerShape(26.dp), spotColor = theme.shadow, ambientColor = theme.shadow)
                .clip(RoundedCornerShape(26.dp))
                .background(theme.bg)
                .border(1.dp, theme.border, RoundedCornerShape(26.dp))
                .drawBehind {
                    // 顶部高光（沉浸光感）
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
            // 滑动选中指示底衬
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(itemW)
                    .height(58.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(theme.selectedBg),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                items.forEachIndexed { index, item ->
                    val selected = mainState.selectedPage == index
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "dock-icon-$index",
                    )
                    Column(
                        modifier = Modifier
                            .width(itemW)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { mainState.animateToPage(index) }
                            .padding(top = 8.dp, bottom = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(contentAlignment = Alignment.TopCenter) {
                            if (selected) {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .drawBehind {
                                            drawRect(
                                                brush = Brush.radialGradient(
                                                    listOf(theme.selected.copy(alpha = 0.25f), Color.Transparent),
                                                ),
                                            )
                                        },
                                )
                            }
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(item.label),
                                tint = if (selected) theme.selected else theme.unselected,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        if (selected) translationY = -1.dp.toPx()
                                    },
                            )
                            val badge = navigationBadgeFor(index, navigationBadge, floating = true)
                            if (badge != null) {
                                Box(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)) { badge() }
                            }
                        }
                        Text(
                            text = stringResource(item.label),
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) theme.selected else theme.unselected,
                        )
                    }
                }
            }
        }
    }
}


/** 每套皮肤各自的专属抽屉风格（跟随皮肤自身的明暗与设计语言） */
object AgDockThemes {

    /** FoxSu Redesign：暖白胶囊 + 品牌紫/品红（晨曦玻璃） */
    val redesign = DockTheme(
        bg = Color(0xC7FFFFFF), border = Color(0x99FFFFFF), shadow = Color(0x2E32286E),
        selected = Color(0xFF6C5CE7), unselected = Color(0xFF66647A),
        selectedBg = Color(0x1A6C5CE7), badge = Color(0xFFEC4899), glass = true,
    )

    /** Pages：极简留白（亮）→ 墨面（暗），单一蓝点 */
    @Composable
    fun fresh(): DockTheme {
        val night = me.weishu.kernelsu.ui.theme.ColorMode
            .fromValue(me.weishu.kernelsu.data.repository.SettingsRepositoryImpl().themeMode).isDark
        return if (night) DockTheme(
            bg = Color(0xF219191F), border = Color(0x29FFFFFF), shadow = Color(0x66000000),
            selected = Color(0xFF7DA9FF), unselected = Color(0xFF8A8F9C),
            selectedBg = Color(0x1F2D6BFF), badge = Color(0xFF2D6BFF),
        ) else DockTheme(
            bg = Color(0xF6FFFFFF), border = Color(0x14111111), shadow = Color(0x1A111111),
            selected = Color(0xFF2D6BFF), unselected = Color(0xFF9AA0B0),
            selectedBg = Color(0x142D6BFF), badge = Color(0xFF2D6BFF),
        )
    }

    /** Editorial：纸面质感 + 墨描边 + 信号橙（恒纸色） */
    val editorial = DockTheme(
        bg = Color(0xFFFFFCF6), border = Color(0x471D1C22), shadow = Color(0x33282418),
        selected = Color(0xFFE8502E), unselected = Color(0xFF9C968B),
        selectedBg = Color(0x1AE8502E), badge = Color(0xFFE8502E),
    )

    /** Daylight：白玻璃（亮）→ 炭玻璃（暗），紫罗兰 + 薄荷点缀 */
    @Composable
    fun glass(): DockTheme {
        val dark = me.weishu.kernelsu.ui.theme.glassTokens().isDark
        return if (dark) DockTheme(
            bg = Color(0xE614141E), border = Color(0x2EFFFFFF), shadow = Color(0x66000000),
            selected = Color(0xFF9C90FF), unselected = Color(0xFF7C8294),
            selectedBg = Color(0x338B7BFF), badge = Color(0xFF4DE2C5), glass = true,
        ) else DockTheme(
            bg = Color(0xCCFFFFFF), border = Color(0x33FFFFFF), shadow = Color(0x2E32286E),
            selected = Color(0xFF6A57F5), unselected = Color(0xFF9AA0B0),
            selectedBg = Color(0x1A6A57F5), badge = Color(0xFF0FA98B), glass = true,
        )
    }

    /** Nocturne：OLED 纯黑胶囊 + 紫罗兰极光（恒暗） */
    val nocturne = DockTheme(
        bg = Color(0xF2101218), border = Color(0x26FFFFFF), shadow = Color(0x66000000),
        selected = Color(0xFF8B7BFF), unselected = Color(0xFF5D6376),
        selectedBg = Color(0x2E8B7BFF), badge = Color(0xFF4DE2C5), glass = true,
    )
}
