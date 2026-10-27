package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme

/**
 * 底栏「更多」面板 —— 底栏只保留三个图标（主页 / 超级用户 / 更多），
 * 其余页面（模块、设置）在这里列出：入口一个不少，只是把图标收进二级面板。
 */
object MoreDestinations {
    const val Module = 2
    const val Setting = 3
}

private data class MoreItem(
    val page: Int,
    val label: Int,
    val icon: ImageVector,
)

@Composable
fun MoreDestinationsSheet(
    visible: Boolean,
    currentTab: Int,
    badge: NavigationBadgeState,
    onDismiss: () -> Unit,
    onOpenTab: (Int) -> Unit,
) {
    if (!visible) return

    val dark = isInDarkTheme()
    val sheetBg = if (dark) Color(0xFF1B1C20) else Color(0xFFF7F6FA)
    val itemBg = if (dark) Color(0xFF26272C) else Color(0xFFFFFFFF)
    val itemBgActive = if (dark) Color(0xFF33324A) else Color(0xFFEDEBFA)
    val fg = if (dark) Color(0xFFF2F1F6) else Color(0xFF1B1B22)
    val fgDim = if (dark) Color(0xFF9C9AA8) else Color(0xFF6B6879)
    val accent = if (dark) Color(0xFFB9AEFF) else Color(0xFF5B4FD6)

    val items = listOf(
        MoreItem(MoreDestinations.Module, R.string.module, Icons.Rounded.Extension),
        MoreItem(MoreDestinations.Setting, R.string.settings, Icons.Rounded.Settings),
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(sheetBg)
                    .clickable(
                        indication = null,
                        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                        onClick = {},
                    )
                    .padding(vertical = 18.dp),
            ) {
                // 顶部小把手
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(fgDim.copy(alpha = 0.35f)),
                )
                Text(
                    text = stringResource(R.string.nav_more),
                    modifier = Modifier.padding(start = 22.dp, top = 16.dp, bottom = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = fgDim,
                )
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { item ->
                        val selected = currentTab == item.page
                        val count = when (item.page) {
                            MoreDestinations.Module -> badge.moduleUpdatableCount
                                .takeIf { it > 0 } ?: badge.moduleEnabledCount

                            else -> 0
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) itemBgActive else itemBg)
                                .clickable {
                                    onOpenTab(item.page)
                                    onDismiss()
                                }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (selected) accent else fg,
                            )
                            Text(
                                text = stringResource(item.label),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp),
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) accent else fg,
                            )
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
