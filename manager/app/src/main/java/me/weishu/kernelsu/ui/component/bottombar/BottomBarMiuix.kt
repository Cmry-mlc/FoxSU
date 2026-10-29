package me.weishu.kernelsu.ui.component.bottombar

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import me.weishu.kernelsu.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ui.LocalMainPagerState

@Composable
fun BottomBarMiuix(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    navigationBadge: NavigationBadgeState,
    modifier: Modifier,
) {
    val fullFeatured = Natives.isFullFeatured()
    if (!fullFeatured) return

    val mainState = LocalMainPagerState.current
    val items = BottomBarDestination.entries.toList()

    // 晨曦玻璃 Dock（设计稿 .dock）：悬浮毛玻璃胶囊 + 4 项 + 品红徽标
    val navInset = androidx.compose.foundation.layout.WindowInsets.navigationBars
        .asPaddingValues().calculateBottomPadding()
    val bottomPad = if (navInset != 0.dp) 10.dp + navInset else 30.dp
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPad)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xC7FFFFFF))
                .border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(26.dp))
                .shadow(14.dp, RoundedCornerShape(26.dp), spotColor = Color(0x2E32286E), ambientColor = Color(0x2E32286E))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = mainState.selectedPage == index
                Column(
                    modifier = Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            if (selected) Color(0x1A6C5CE7)
                            else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { mainState.animateToPage(index) }
                        .padding(top = 8.dp, bottom = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    val badge = navigationBadgeFor(index, navigationBadge, floating = true)
                    Box(contentAlignment = Alignment.TopCenter) {
                        val iconT = if (selected) AgDock.vio else AgDock.tx3
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.label),
                            tint = iconT,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    if (selected) {
                                        translationY = -1.dp.toPx()
                                        scaleX = 1.08f
                                        scaleY = 1.08f
                                    }
                                },
                        )
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
                        color = if (selected) AgDock.vio else AgDock.tx3,
                    )
                }
            }
        }
    }
}

/** Dock 专用色 */
private object AgDock {
    val vio = Color(0xFF6C5CE7)
    val tx3 = Color(0xFF9C9AAE)
}


enum class BottomBarDestination(
    @get:StringRes val label: Int,
    /** 线性图标：Miuix / Material 等仍走描边语言的皮肤使用 */
    val icon: ImageVector,
    /** 实心图标：Redesign 胶囊（品牌稿）使用，视觉重量更足 */
    val filledIcon: ImageVector = icon,
) {
    Home(R.string.home, Icons.Rounded.Cottage, Icons.Filled.Home),
    SuperUser(R.string.superuser, Icons.Rounded.Security, Icons.Filled.Security),
    Module(R.string.module, Icons.Rounded.Extension, Icons.Filled.Extension),
    Setting(R.string.settings, Icons.Rounded.Settings, Icons.Filled.Settings)
}

internal fun navigationBadgeFor(
    index: Int,
    state: NavigationBadgeState,
    floating: Boolean = false,
): (@Composable () -> Unit)? {
    val badge = badgeFor(index, state) ?: return null
    return when (badge.tone) {
        BadgeTone.Alert -> {
            {
                Badge(containerColor = Color(0xFFEC4899), contentColor = Color.White) {
                    Text(badge.count.toString())
                }
            }
        }

        BadgeTone.Accent -> {
            {
                Badge(
                    containerColor = if (floating) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.primary,
                    contentColor = if (floating) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onPrimary,
                ) {
                    Text(badge.count.toString())
                }
            }
        }
    }
}
