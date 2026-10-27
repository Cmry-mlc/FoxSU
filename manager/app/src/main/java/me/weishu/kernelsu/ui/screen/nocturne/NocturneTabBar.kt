package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.MoreDestinations
import me.weishu.kernelsu.ui.component.bottombar.MoreDestinationsSheet
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState

/**
 * 底栏 —— 严格对照设计稿：
 * 左右 18 / 底 16 / 高 64 / r22 玻璃 rgba(20,22,30,.72)，
 * 4 tab 平分，选中 violet + 顶部 24×2.5 violet→mint 指示条 + 发光，右上 violetDeep 数字徽标。
 */
@Composable
fun NocturneTabBarScreen(
    currentTab: Int,
    badge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = ncTokens()
    // 底栏只保留三个图标：主页 / 超级用户 / 更多；模块与设置收进「更多」面板
    var showMore by remember { mutableStateOf(false) }
    val moreSelected = currentTab >= MoreDestinations.Module
    val tabs = listOf(
        Triple("主页", R.drawable.nc_home, 0),
        Triple("超级用户", R.drawable.nc_shield, 1),
        Triple("更多", R.drawable.nc_more, 2),
    )
    Row(
        modifier
            .padding(horizontal = 18.dp)
            .padding(bottom = 16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(t.navBg)
            .border(1.dp, t.navBorder, RoundedCornerShape(22.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (label, icon, index) ->
            val selected = if (index == 2) moreSelected else currentTab == index
            val count = when (index) {
                1 -> badge.superuserCount
                // 「更多」承载模块页，模块角标提示挪到这里
                2 -> badge.moduleEnabledCount
                else -> 0
            }
            val src = remember { MutableInteractionSource() }
            val pressed by src.collectIsPressedAsState()
            val sc by animateFloatAsState(
                if (pressed) 0.86f else 1f,
                spring(0.6f, Spring.StiffnessMedium),
                label = "tab",
            )
            val lift by animateFloatAsState(
                if (selected) (-1f) else 0f,
                spring(0.6f, Spring.StiffnessMediumLow),
                label = "lift",
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = src, indication = null) {
                        if (index == 2) showMore = true else onOpenTab(index)
                    },
                contentAlignment = Alignment.Center,
            ) {
                // 顶部指示条 24×2.5
                if (selected) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 6.dp)
                            .size(24.dp, 2.5.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(t.navIndicator))
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (3f + lift).dp).scale(sc),
                ) {
                    Box {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = label,
                            tint = if (selected) t.navActiveIcon else t.navInactive,
                            modifier = Modifier.size(21.dp),
                        )
                        if (count > 0) {
                            NcBadgeDot(
                                count = count,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-2).dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) t.navActiveText else t.navInactive,
                    )
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
