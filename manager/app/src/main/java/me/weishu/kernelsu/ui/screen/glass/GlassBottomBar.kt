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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
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
    val items = listOf(
        BottomItem(R.drawable.ic_d_home, R.string.home, 0, t.cyan, 0),
        BottomItem(R.drawable.ic_d_shield, R.string.superuser, 1, t.violet, badge.superuserCount),
        BottomItem(R.drawable.ic_d_grid, R.string.module, 2, t.pink, badge.moduleEnabledCount),
        BottomItem(R.drawable.ic_d_tune, R.string.settings, 3, t.blue, 0),
    )
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .shadow(
                    14.dp,
                    RoundedCornerShape(99.dp),
                    ambientColor = t.dockShadow,
                    spotColor = t.dockShadow,
                )
                .clip(RoundedCornerShape(99.dp))
                .background(t.dockBg)
                .border(1.dp, t.dockBorder, RoundedCornerShape(99.dp))
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.tab == currentTab
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .clickable { onOpenTab(item.tab) }
                            .padding(horizontal = 13.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = iconTint,
                        )
                        Text(
                            text = stringResource(item.labelRes),
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp,
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
                                .offset(y = (-7).dp)
                                .size(width = 22.dp, height = 3.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(t.dockGrad),
                        )
                    }
                    // .n 角标：色彩与所在导航项图标一致
                    if (item.count > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 1.dp, end = 8.dp)
                                .heightIn(min = 13.dp)
                                .clip(CircleShape)
                                .background(item.accent)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item.count.toString(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class BottomItem(
    val iconRes: Int,
    val labelRes: Int,
    val tab: Int,
    val accent: Color,
    val count: Int,
)
