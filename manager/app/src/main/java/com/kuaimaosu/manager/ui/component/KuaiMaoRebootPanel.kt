package com.kuaimaosu.manager.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DeveloperBoard
import androidx.compose.material.icons.twotone.PowerSettingsNew
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.SettingsBackupRestore
import androidx.compose.material.icons.twotone.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.LocalKuaiMaoPalette
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import org.koin.compose.koinInject

/**
 * 重启面板的展开状态。
 *
 * 状态放在 MainScreen 上（面板也画在那里），标题栏里的重启按钮只负责 flip 这个开关，
 * 所以四个页签点哪儿都是同一个面板、同一份状态。
 */
class RebootPanelState {
    var expanded by mutableStateOf(false)
        private set

    /**
     * 标题栏图标簇的下边缘（窗口坐标，px）。
     *
     * 面板要贴着它往下展开，所以位置不写死：首页是 44dp 的头像行、
     * 其余页是 24sp 大标题 + 副标题，高度本来就不一样，量出来才准，
     * 也才能保证不管哪一页都不压到标题文字。
     */
    var anchorBottomPx by mutableFloatStateOf(0f)

    fun toggle() {
        expanded = !expanded
    }

    fun dismiss() {
        expanded = false
    }
}

val LocalRebootPanel = staticCompositionLocalOf { RebootPanelState() }

/** 图标簇下边缘与面板顶边之间的空隙。 */
private val PANEL_TOP_GAP = 8.dp

/** 拿不到锚点时的兜底：状态栏 + 标题行。 */
private val PANEL_FALLBACK_TOP = 58.dp

private val PANEL_RADIUS = 13.dp

/** 竖排面板定宽：足够放下最长标签（Bootloader），贴右边缘。 */
private val PANEL_WIDTH = 76.dp

private data class RebootOption(
    val label: String,
    val reason: String,
    val icon: ImageVector,
    val danger: Boolean = false,
)

private val REBOOT_OPTIONS = listOf(
    RebootOption("重启", "reboot", Icons.TwoTone.RestartAlt),
    RebootOption("软重启", "soft_reboot", Icons.TwoTone.Refresh),
    RebootOption("Recovery", "recovery", Icons.TwoTone.SettingsBackupRestore),
    RebootOption("Bootloader", "bootloader", Icons.TwoTone.DeveloperBoard),
    RebootOption("EDL", "edl", Icons.TwoTone.Usb, danger = true),
    RebootOption("关机", "poweroff", Icons.TwoTone.PowerSettingsNew, danger = true),
)

/**
 * 重启面板：从标题栏右侧图标簇正下方、贴右边缘竖着抽出来的一列动作。
 *
 * 为什么不做成底部抽屉：那是半屏浮层，点一次要「上滑—选—收起」三步，而且面板
 * 和底栏的导航职责混在一起。现在它就在重启图标正下方，高度从 0 展开到内容高
 * （顶边对齐图标簇，所以视觉上是「贴着右边往下拉开」），六个动作一列看完，选完即走。
 *
 * 位置不是写死的：图标簇每次布局都会把自己的下沿报上来（[RebootPanelState.anchorBottomPx]），
 * 面板贴在它下方 8dp 展开。所以首页的 44dp 头像行、其余页的 24sp 大标题 + 副标题，
 * 不管哪一页、副标题有没有换行，标题文字都不会被压住。
 */
@Composable
fun KuaiMaoRebootPanel(
    state: RebootPanelState,
    onReboot: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardConfig: CardConfig = koinInject()
    val onWallpaper = cardConfig.isCustomBackgroundEnabled
    val lightCanvas = LocalKuaiMaoPalette.current.canvasStage.luminance() >= 0.5f
    val shape = RoundedCornerShape(PANEL_RADIUS)
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var hostTopPx by remember { mutableFloatStateOf(0f) }
    // 锚点（px，窗口坐标）→ 相对本层的偏移；还没量到图标簇就先按状态栏兜底。
    val panelTop = if (state.anchorBottomPx > 0f && hostTopPx > 0f) {
        with(density) {
            (state.anchorBottomPx - hostTopPx).toDp() + PANEL_TOP_GAP
        }
    } else {
        statusBarTop + PANEL_FALLBACK_TOP
    }

    if (state.expanded) {
        BackHandler { state.dismiss() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { hostTopPx = it.boundsInWindow().top },
    ) {
        // 点空白处收起。这一层只在展开时存在，收起后页面照常可点。
        if (state.expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { state.dismiss() },
            )
        }

        AnimatedVisibility(
            visible = state.expanded,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(start = 16.dp, end = 20.dp, top = panelTop),
            // 顶边钉住图标簇，高度 0 → 内容高：看上去就是从重启按钮里往下抽出来。
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 900f),
            ) + fadeIn(tween(durationMillis = 140)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
            ) + fadeOut(tween(durationMillis = 120)),
        ) {
            Column(
                modifier = Modifier
                    .width(PANEL_WIDTH)
                    .clip(shape)
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
                            Modifier.border(
                                width = 1.dp,
                                color = KuaiMaoColors.line.copy(alpha = 0.8f),
                                shape = shape,
                            )
                        },
                    )
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                REBOOT_OPTIONS.forEach { option ->
                    RebootOptionItem(
                        option = option,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            state.dismiss()
                            onReboot(option.reason)
                        },
                    )
                }
            }
        }
    }
}

/** 一格里两行：17dp 图标 + 9.5sp 标签，和标题栏图标簇同一套字号与档位。 */
@Composable
private fun RebootOptionItem(
    option: RebootOption,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (option.danger) KuaiMaoColors.danger else KuaiMaoColors.textLabel
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PANEL_RADIUS - 3.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = option.label,
            color = if (option.danger) KuaiMaoColors.danger else KuaiMaoColors.textMuted,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
