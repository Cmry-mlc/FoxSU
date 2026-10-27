package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState

/** Nocturne 底栏：64dp 高 r22 玻璃 nav，选中项顶部 24×2.5 violet→mint 指示条 + 图标发光。 */
@Composable
fun NocturneTabBarScreen(
    currentTab: Int,
    badge: NavigationBadgeState,
    onOpenTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = ncTokens()
    val items = listOf(
        Triple(R.drawable.ic_d_home, R.string.home, 0),
        Triple(R.drawable.ic_d_shield, R.string.superuser, 1),
        Triple(R.drawable.ic_d_grid, R.string.module, 2),
        Triple(R.drawable.ic_d_sliders, R.string.settings, 3),
    )
    fun badgeCount(tab: Int): Int = when (tab) {
        1 -> badge.superuserCount
        2 -> badge.moduleEnabledCount
        else -> 0
    }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .padding(start = NcDimens.pageX, end = NcDimens.pageX, bottom = 16.dp)
                .shadow(
                    14.dp,
                    RoundedCornerShape(NcRadius.nav),
                    ambientColor = if (t.dark) Color(0x8C000000) else Color(0x24203060),
                    spotColor = if (t.dark) Color(0x8C000000) else Color(0x24203060),
                )
                .clip(RoundedCornerShape(NcRadius.nav))
                .background(if (t.dark) NcColors.navBg else Color(0xD1FFFFFF))
                .border(1.dp, t.line, RoundedCornerShape(NcRadius.nav)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (iconRes, labelRes, tab) ->
                val selected = tab == currentTab
                val lift by animateFloatAsState(
                    targetValue = if (selected) -1f else 0f,
                    animationSpec = tween(240),
                    label = "tabLift",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    // 选中项顶部 24×2.5 渐变指示条
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = 3.dp)
                                .size(width = 24.dp, height = 2.5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(t.violet, t.mint)))
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenTab(tab) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier
                                .size(21.dp)
                                .graphicsLayer { translationY = lift * 2f },
                            tint = if (selected) t.violet else t.iconTint,
                        )
                        Text(
                            text = stringResource(labelRes),
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) t.text1 else t.text3,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    // 徽标
                    val count = badgeCount(tab)
                    if (count > 0) {
                        val badgeSize = if (count < 10) 15.dp else 17.dp
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 8.dp)
                                .size(badgeSize)
                                .clip(CircleShape)
                                .background(NcColors.violetDeep),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (count > 99) "99+" else count.toString(),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
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
}
