package me.weishu.kernelsu.ui.screen.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * .dock：悬浮胶囊底栏（设计稿 fit-content + 居中，外层 padding 6，item padding 8×15）。
 * 选中项：白色 pill + 顶部上移 7dp 的 22×3 霓虹条 + 深色图标。
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
        Triple(R.drawable.ic_d_home, R.string.home, 0),
        Triple(R.drawable.ic_d_shield, R.string.superuser, 1),
        Triple(R.drawable.ic_d_grid, R.string.module, 2),
        Triple(R.drawable.ic_d_tune, R.string.settings, 3),
    )
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 14.dp)
                .shadow(
                    14.dp,
                    RoundedCornerShape(99.dp),
                    ambientColor = Color(0x2E5A5578),
                    spotColor = Color(0x2E5A5578),
                )
                .clip(RoundedCornerShape(99.dp))
                .background(if (t.isDark) Color(0xD9101018) else Color(0xB3FFFFFF))
                .border(
                    1.dp,
                    if (t.isDark) Color(0x14FFFFFF) else Color(0xE6FFFFFF),
                    RoundedCornerShape(99.dp),
                )
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (iconRes, labelRes, tab) ->
                val selected = tab == currentTab
                val badgeCount = when (tab) {
                    1 -> badge.superuserCount
                    2 -> badge.moduleEnabledCount
                    else -> 0
                }
                Box {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selected) {
                                    if (t.isDark) Color(0x14FFFFFF) else Color(0xF2FFFFFF)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { onOpenTab(tab) }
                            .padding(horizontal = 15.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = when {
                                selected -> t.ink
                                t.isDark -> Color(0xFF6A677A)
                                else -> Color(0xFFA3A0B2)
                            },
                        )
                        Text(
                            text = stringResource(labelRes),
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                            color = if (selected) t.ink else t.sub,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    // .ditem.on::before —— 顶部霓虹条
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
                    if (badgeCount > 0) {
                        Text(
                            text = badgeCount.toString(),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 10.dp)
                                .clip(CircleShape)
                                .background(t.flame)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}
