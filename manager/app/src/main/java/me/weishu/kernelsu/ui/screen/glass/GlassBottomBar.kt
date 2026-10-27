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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.bottombar.NavigationBadgeState
import me.weishu.kernelsu.ui.theme.glassTokens

/** Glass dock 悬浮底栏：胶囊玻璃条 + 选中顶部霓虹渐变条。 */
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
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    if (t.isDark) Color(0xD9101018) else Color(0xB3FFFFFF)
                )
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
                Box(modifier = Modifier.clip(RoundedCornerShape(18.dp))) {
                    Column(
                        modifier = Modifier
                            .clickable { onOpenTab(tab) }
                            .padding(horizontal = 15.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 3.dp)
                                    .size(width = 22.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(t.dockGrad),
                            )
                        } else {
                            Spacer(Modifier.height(6.dp))
                        }
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = if (selected) t.ink else if (t.isDark) Color(0xFF6A677A) else Color(0xFFA3A0B2),
                        )
                        Text(
                            text = stringResource(labelRes),
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                            color = if (selected) t.ink else t.sub,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (badgeCount > 0) {
                        Text(
                            text = badgeCount.toString(),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
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
