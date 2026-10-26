package me.weishu.kernelsu.ui.component.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.data.settings.CustomizationStore
import me.weishu.kernelsu.ui.component.freshCardBorder
import me.weishu.kernelsu.ui.component.glassNeedsLightText
import me.weishu.kernelsu.ui.component.isNightScheme
import me.weishu.kernelsu.ui.theme.LaunchTint
import me.weishu.kernelsu.ui.component.freshCardColor
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalMainPagerState

private data class FreshNavItem(
    val label: Int,
    val selectedIcon: Int,
    val unselectedIcon: Int,
)

@Composable
fun BottomBarFresh(navigationBadge: NavigationBadgeState) {
    if (!Natives.isFullFeatured()) return

    val mainPagerState = LocalMainPagerState.current
    val custom = CustomizationStore.current

    val items = listOf(
        FreshNavItem(R.string.home, R.drawable.ic_nav_home_fill, R.drawable.ic_nav_home),
        FreshNavItem(R.string.superuser, R.drawable.ic_nav_shield_fill, R.drawable.ic_nav_shield),
        FreshNavItem(R.string.module, R.drawable.ic_nav_module_fill, R.drawable.ic_nav_module),
        FreshNavItem(R.string.settings, R.drawable.ic_nav_settings_fill, R.drawable.ic_nav_settings),
    )

    // 底栏每个图标各自取一套七彩颜色（每次启动顺序随机），选中实色、未选中淡化
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = navigationBarPadding + 12.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val barShape = RoundedCornerShape(custom.bottomBarRadius.dp)
        val capsule = custom.bottomBarStyle != 1
        // 视频样式：半透明悬浮条（自定义卡片色也保持通透）；沉浸光感更透并叠柔光
        val immersive = custom.bottomBarImmersive
        val nightBar = isNightScheme()
        val barColor = freshCardColor().let {
            when {
                // 玻璃模式：和卡片同一套通透度，深色壁纸下更透
                custom.cardGlass -> if (glassNeedsLightText()) {
                    Color.White.copy(alpha = 0.14f)
                } else {
                    Color.White.copy(alpha = 0.52f)
                }
                nightBar -> if (immersive) {
                    Color(0xFF17181A).copy(alpha = 0.86f)
                } else {
                    Color(0xFF1B1C1E).copy(alpha = 0.94f)
                }
                immersive -> it.copy(alpha = 0.66f)
                it.alpha > 0.9f -> it.copy(alpha = 0.84f)
                else -> it
            }
        }
        Surface(
            shape = barShape,
            color = barColor,
            border = freshCardBorder(),
            shadowElevation = if (custom.cardGlass) 0.dp else 12.dp,
            modifier = if (capsule) Modifier else Modifier.fillMaxWidth(),
        ) {
            Box {
                if (immersive || custom.cardGlass) {
                    // 鸿蒙式柔光：上半部一层白高光，底部自然淡出
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.White.copy(alpha = 0.34f),
                                    0.55f to Color.White.copy(alpha = 0.08f),
                                    1f to Color.Transparent,
                                )
                            ),
                    )
                }
            Row(
                modifier = Modifier
                    .then(if (capsule) Modifier else Modifier.fillMaxWidth())
                    .padding(horizontal = if (capsule) 10.dp else 8.dp, vertical = 5.dp),
                horizontalArrangement = if (custom.bottomBarStyle == 1) {
                    Arrangement.Center
                } else {
                    Arrangement.Center
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val selected = mainPagerState.selectedPage == index
                    val onClick = { if (!selected) mainPagerState.animateToPage(index) }
                    if (custom.bottomBarStyle == 1) {
                        DrawerNavItem(
                            item = item,
                            selected = selected,
                            badge = badgeFor(index, navigationBadge),
                            accent = LaunchTint.navColor(index),
                            onClick = onClick,
                        )
                    } else {
                        FreshNavItemView(
                            item = item,
                            selected = selected,
                            badge = badgeFor(index, navigationBadge),
                            accent = LaunchTint.navColor(index),
                            onClick = onClick,
                        )
                    }
                }
            }
            }
        }
    }
}

/**
 * Drawer style tab: the selected tab expands into a pill that shows icon + label, the others
 * collapse to bare icons. Width, colour and label all animate, so switching looks like a drawer
 * sliding open.
 */
@Composable
private fun DrawerNavItem(
    item: FreshNavItem,
    selected: Boolean,
    badge: NavBadge?,
    accent: Color,
    onClick: () -> Unit,
) {
    val width by animateDpAsState(
        targetValue = if (selected) 124.dp else 56.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "drawerWidth",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(durationMillis = 240),
        label = "drawerPill",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else accent.copy(alpha = 0.52f),
        animationSpec = tween(durationMillis = 240),
        label = "drawerContent",
    )

    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(width)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(pillColor)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (badge != null) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = if (badge.tone == BadgeTone.Alert) {
                            MaterialTheme.colorScheme.error
                        } else {
                            accent
                        },
                        contentColor = Color.White,
                    ) {
                        Text(badge.count.toString())
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                    contentDescription = stringResource(item.label),
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Icon(
                painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                contentDescription = stringResource(item.label),
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = tween(180)) +
                expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                    expandFrom = Alignment.Start,
                ),
            exit = fadeOut(animationSpec = tween(90)) +
                shrinkHorizontally(animationSpec = tween(120), shrinkTowards = Alignment.Start),
        ) {
            Text(
                text = stringResource(item.label),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun FreshNavItemView(
    item: FreshNavItem,
    selected: Boolean,
    badge: NavBadge?,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else accent.copy(alpha = 0.52f),
        animationSpec = tween(durationMillis = 220),
        label = "navContent",
    )
    // 视频样式：无选中底色，用按压缩放 + 选中弹性缩放表达状态
    val targetScale = when {
        pressed -> 0.86f
        selected -> 1.06f
        else -> 1f
    }
    val iconScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = if (selected) 0.45f else 0.6f, stiffness = 480f),
        label = "navScale",
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (badge != null) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = if (badge.tone == BadgeTone.Alert) {
                            MaterialTheme.colorScheme.error
                        } else {
                            accent
                        },
                        contentColor = Color.White,
                    ) {
                        Text(badge.count.toString())
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                    contentDescription = stringResource(item.label),
                    tint = contentColor,
                    modifier = Modifier
                        .size(23.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        } else {
            Icon(
                painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                contentDescription = stringResource(item.label),
                tint = contentColor,
                modifier = Modifier
                    .size(23.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
        }
        Text(
            text = stringResource(item.label),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale.coerceAtMost(1f)
                scaleY = iconScale.coerceAtMost(1f)
            },
        )
    }
}
