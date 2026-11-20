package com.kuaimaosu.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.data.notify.KuaiMaoNotificationStore
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.screen.main.kuaimaoNotices
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.LocalKuaiMaoPalette
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.kuaimaosu.manager.ui.component.LocalRebootPanel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.kuaimaosu.manager.ui.viewmodel.HomeViewModel

/** 动作条整体高度：与各页标题右侧原来那枚 40dp 的按钮同高。 */
private val ACTIONS_HEIGHT = 40.dp

/** 单个图标的点击格宽度：40dp 一格，两个图标之间自然留出 21dp 空隙。 */
private val ACTIONS_SLOT_WIDTH = 40.dp

/** 圆角与通知按钮保持一致（13dp），整条看起来是一块圆角矩形，不是胶囊。 */
private val ACTIONS_RADIUS = 13.dp

private val ACTIONS_SHAPE = RoundedCornerShape(ACTIONS_RADIUS)

/**
 * 标题栏右侧的全局动作条：通知铃铛 + 重启，装进同一块 13dp 圆角矩形里。
 *
 * 为什么合在一起：以前铃铛是首页标题右上角一枚独立的 40dp 方块，重启则被塞在
 * 底栏中央（胶囊栏一个主题色小胶囊、传统栏一个「图标 + 文字」的页签）。
 * 两处风格都不一样，而且底栏中间的「动作」和两侧的「切页」混在一排也分不清。
 * 现在统一收进标题栏：一块圆角底、两枚图标，底栏只负责切换页面。
 *
 * 页面自己的动作（超级用户的筛选 / 日志、模块页的更多）通过 [pageActions]
 * 排在同一条动作条的左端，保持「一块控件」的观感，不再是一排孤立方块。
 *
 * @param pageActions 页面私有动作，逐个用 [KuaiMaoTopActionSlot] 渲染。
 */
@Composable
fun KuaiMaoTopActions(
    modifier: Modifier = Modifier,
    pageActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val rebootPanel = LocalRebootPanel.current
    // 展开时把重启图标转 90° 并换成主题色：这一个动作既是入口也是开关。
    val expanded = rebootPanel.expanded
    val rebootSpin by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "rebootSpin",
    )
    val cardConfig: CardConfig = koinInject()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val unreadCount = kuaimaoNotices(uiState).count { it.id !in KuaiMaoNotificationStore.readIds }

    // 自定义壁纸下不要实色块：夜间 canvasCard 是 #1C1C1C，压在壁纸上就是
    // 「图标外面套了一个黑方块」。现在连那圈描边也去掉了，只留一层极薄的雾，
    // 图标干净地浮在壁纸上，不会再出现黑框。
    val onWallpaper = cardConfig.isCustomBackgroundEnabled
    val lightCanvas = LocalKuaiMaoPalette.current.canvasStage.luminance() >= 0.5f

    Row(
        modifier = modifier
            .height(ACTIONS_HEIGHT)
            .clip(ACTIONS_SHAPE)
            .background(
                if (onWallpaper) Color.Transparent else KuaiMaoColors.canvasCard,
            )
            .then(
                if (onWallpaper) {
                    Modifier.renderBackgroundBlur(
                        tintColor = if (lightCanvas) Color.White else Color.Black,
                        tintAlpha = if (lightCanvas) 0.26f else 0.10f,
                    )
                } else {
                    // 实色页面还是留一条发丝边：白卡压在浅灰底上，全靠明度差
                    // 分界会显得发虚。壁纸下没有这条线——那里才是「黑框」的来源。
                    Modifier.border(
                        width = 1.dp,
                        color = KuaiMaoColors.line.copy(alpha = 0.8f),
                        shape = ACTIONS_SHAPE,
                    )
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pageActions?.invoke(this)

        KuaiMaoTopActionSlot(
            onClick = {
                rebootPanel.dismiss()
                navigator.push(Route.Notifications)
            },
        ) {
            // 用项目内自绘的单色铃铛：material icon 在部分设备上会把
            // 矢量填充区域带成白色方底，换成纯 path 后不会有底板。
            Icon(
                painter = painterResource(R.drawable.ic_kuaimao_bell),
                contentDescription = "通知",
                tint = KuaiMaoColors.textLabel,
                modifier = Modifier.size(19.dp),
            )
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(KuaiMaoColors.danger),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        fontSize = 7.5.sp,
                        lineHeight = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        KuaiMaoTopActionSlot(
            onClick = { rebootPanel.toggle() },
        ) {
            Icon(
                imageVector = KuaiMaoRestartIcon,
                contentDescription = stringResource(R.string.reboot),
                tint = if (expanded) KuaiMaoColors.brand else KuaiMaoColors.textLabel,
                modifier = Modifier
                    .size(19.dp)
                    .graphicsLayer { rotationZ = rebootSpin },
            )
        }
    }
}

/**
 * 动作条里的一格。
 *
 * 点击区域固定 40dp 见方、图标 19dp，这样铃铛、重启、以及页面自己的动作
 * 在视觉上是同一档大小、同一档间距；角标可以直接 align 到这一格上。
 */
@Composable
fun KuaiMaoTopActionSlot(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .width(ACTIONS_SLOT_WIDTH)
            .height(ACTIONS_HEIGHT)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
