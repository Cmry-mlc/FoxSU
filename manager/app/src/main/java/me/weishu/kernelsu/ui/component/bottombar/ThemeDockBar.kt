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
