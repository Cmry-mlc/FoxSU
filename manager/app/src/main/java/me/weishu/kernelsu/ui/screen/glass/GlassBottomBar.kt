package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.MoreDestinations
import me.weishu.kernelsu.ui.component.bottombar.MoreDestinationsSheet
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.theme.glassTokens

/**
 * .dock 悬浮胶囊底栏 —— 严格按设计稿：
 *   bottom:14px; 居中; padding:6px; border-radius:99px
 *   暗色 bg rgba(16,16,24,.85) + 1px rgba(255,255,255,.08) + 阴影 0 14 34 rgba(0,0,0,.6)
 *   亮色 bg rgba(255,255,255,.7)  + 1px rgba(255,255,255,.9)  + 阴影 0 14 34 rgba(90,85,120,.25)
 *   .ditem padding 8×15, radius 18, icon 17px, font 9px/ls .5px
 *   .ditem.on：pill 底色 + 顶部 22×3 霓虹条（top:-7）+ 文字/图标反色
 *   .n 角标：top2 right10, font 7.5→8.5, 背景取导航项对应霓虹色
 */
@Composable
fun GlassBottomBar(
    currentTab: Int,
    badge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = glassTokens()
    // 底栏只保留三个图标：主页 / 超级用户 / 更多；模块与设置收进「更多」面板
    var showMore by remember { mutableStateOf(false) }
    val moreSelected = currentTab >= MoreDestinations.Module
    val items = listOf(
        BottomItem(R.drawable.ic_d_home, R.string.home, 0, t.cyan, 0),
        BottomItem(R.drawable.ic_d_shield, R.string.superuser, 1, t.violet, badge.superuserCount),
        BottomItem(R.drawable.ic_d_more, R.string.nav_more, 2, t.pink, badge.moduleEnabledCount),
    )
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .shadow(
                    14.dp,
                    RoundedCornerShape(99.dp),
                    ambientColor = t.dockShadow,
                    spotColor = t.dockShadow,
                )
                .clip(RoundedCornerShape(99.dp))
                .background(t.dockBg)
                .border(1.dp, t.dockBorder, RoundedCornerShape(99.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = if (item.tab == 2) moreSelected else item.tab == currentTab
                // 首项左侧、末项右侧的圆角与胶囊外轮廓一致（99dp），避免选中底角戳出胶囊
                val isFirst = index == 0
                val isLast = index == items.lastIndex
                val pillShape = RoundedCornerShape(
                    topStart = if (isFirst) 99.dp else 16.dp,
                    bottomStart = if (isFirst) 99.dp else 16.dp,
                    topEnd = if (isLast) 99.dp else 16.dp,
                    bottomEnd = if (isLast) 99.dp else 16.dp,
                )
                val bg by animateColorAsState(
                    if (selected) t.dockSelectedBg else Color.Transparent,
                    label = "dockBg",
                )
                val fg by animateColorAsState(
                    if (selected) t.dockInk else t.dockSub,
                    label = "dockFg",
                )
                val iconTint by animateColorAsState(
                    if (selected) t.dockInk else t.dockIconOff,
                    label = "dockIcon",
                )
                Box {
                    Column(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(bg)
                            .clickable {
                                if (item.tab == 2) showMore = true else onOpenTab(item.tab)
                            }
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = iconTint,
                        )
                        Text(
                            text = stringResource(item.labelRes),
                            fontSize = 7.5.sp,
                            letterSpacing = 0.3.sp,
                            color = fg,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    // .ditem.on::before —— 顶部 22×3 霓虹条，位于 pill 外 7dp
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-4).dp)
                                .size(width = 22.dp, height = 3.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(t.dockGrad),
                        )
                    }
                    // .n 角标：正圆 + 数字绝对居中（关闭字体上下留白，避免视觉偏移）
                    if (item.count > 0) {
                        val badgeSize = if (item.count < 10) 15.dp else 17.dp
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 5.dp, y = (-3).dp)
                                .size(badgeSize)
                                .clip(CircleShape)
                                .background(item.accent)
                                .border(1.dp, t.dockBg, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (item.count > 99) "99+" else item.count.toString(),
                                color = Color.White,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = if (item.count < 10) 8.sp else 7.sp,
                                    lineHeight = if (item.count < 10) 8.sp else 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    MoreDestinationsSheet(
        visible = showMore,
        currentTab = currentTab,
        badge = badge,
        onDismiss = { showMore = false },
        onOpenTab = onOpenTab,
    )
}

private data class BottomItem(
    val iconRes: Int,
    val labelRes: Int,
    val tab: Int,
    val accent: Color,
    val count: Int,
)
