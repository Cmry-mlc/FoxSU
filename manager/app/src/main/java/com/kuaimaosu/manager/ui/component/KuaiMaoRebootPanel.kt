package com.kuaimaosu.manager.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.LocalKuaiMaoPalette
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import org.koin.compose.koinInject

/**
 * 重启条的展开状态。
 *
 * 状态放在 MainScreen 上（条也画在那里），标题栏里的重启按钮只负责 flip 这个开关，
 * 所以四个页签点哪儿都是同一条、同一份状态。
 */
class RebootPanelState {
    var expanded by mutableStateOf(false)
        private set

    /**
     * 标题栏动作条的右边界（窗口坐标，px）。
     *
     * 展开时这条要把整块动作条（铃铛 + 重启）盖住，所以右端必须和它严丝合缝：
     * 右边界对齐才有「原地换了一组图标」的连续感，不会先跳一下再展开。
     */
    var anchorRightPx by mutableFloatStateOf(0f)

    /** 标题栏动作条的垂直中心（窗口坐标，px）：条与它同高、垂直居中。 */
    var anchorCenterPx by mutableFloatStateOf(0f)

    fun toggle() {
        expanded = !expanded
    }

    fun dismiss() {
        expanded = false
    }
}

val LocalRebootPanel = staticCompositionLocalOf { RebootPanelState() }

/**
 * 展开后的整条宽度。
 *
 * 收起时标题栏右侧只占「铃铛 + 重启」两格（80dp），展开后这条直接把它们盖掉，
 * 可用宽度就从标题栏中间那段 137dp 的窄缝，变成「标题右边界 → 屏幕右边」的
 * 约 200dp：六格各到 26dp、文字能到 7sp，看得清、点得准。
 */
private val BAR_WIDTH = 196.dp

/** 与标题栏动作条同高（40dp），盖上去刚好同一档。 */
private val BAR_HEIGHT = 40.dp

/** 圆角与动作条一致，换过去像同一块控件。 */
private val BAR_RADIUS = 13.dp

/** 最右端的关闭格：正好压在原来那颗重启图标的位置上，点它就是收起。 */
private val CLOSE_SLOT_WIDTH = 36.dp

/** 拿不到锚点时的兜底右边距：页面右内边距。 */
private val BAR_FALLBACK_END = 20.dp

private data class RebootOption(
    val label: String,
    /** 26dp 一格只放得下两个汉字，标签都要压到这个长度。 */
    val short: String,
    val reason: String,
    val icon: ImageVector,
    val danger: Boolean = false,
)

private val REBOOT_OPTIONS = listOf(
    RebootOption("重启", "重启", "reboot", Icons.TwoTone.RestartAlt),
    RebootOption("软重启", "软启", "soft_reboot", Icons.TwoTone.Refresh),
    RebootOption("Recovery", "恢复", "recovery", Icons.TwoTone.SettingsBackupRestore),
    RebootOption("Bootloader", "BL", "bootloader", Icons.TwoTone.DeveloperBoard),
    RebootOption("EDL", "EDL", "edl", Icons.TwoTone.Usb, danger = true),
    RebootOption("关机", "关机", "poweroff", Icons.TwoTone.PowerSettingsNew, danger = true),
)

/**
 * 重启条：展开时原地盖住标题栏右侧的「铃铛 + 重启」，换成六个动作 + 一个关闭格。
 *
 * 为什么覆盖而不是另找地方放：标题栏中间只剩 137dp 的窄缝，六格挤进去每格只有
 * 21dp、文字得压到 6sp，基本看不清；而铃铛 + 重启本身占着 80dp。展开时把这块
 * 让出来，整条可用宽度就有约 200dp，每格 26dp、文字 7sp，不用牺牲可读性。
 *
 * 关闭格钉在最右端、和原来那颗重启图标同一个位置：用户想「再点一下关掉」时，
 * 手指落点就是关闭，不会误触到关机或 EDL。
 *
 * 位置不写死：动作条每次布局都把右边界和垂直中心报上来
 * （[RebootPanelState.anchorRightPx] / [RebootPanelState.anchorCenterPx]），
 * 条按它右对齐展开，四个页签都对得准。
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
    val shape = RoundedCornerShape(BAR_RADIUS)
    val density = LocalDensity.current
    var hostLeftPx by remember { mutableFloatStateOf(0f) }
    var hostTopPx by remember { mutableFloatStateOf(0f) }
    var hostWidthPx by remember { mutableFloatStateOf(0f) }

    if (state.expanded) {
        BackHandler { state.dismiss() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val bounds = it.boundsInWindow()
                hostLeftPx = bounds.left
                hostTopPx = bounds.top
                hostWidthPx = bounds.width.toFloat()
            },
    ) {
        // 右端对齐动作条的右边界：宽度从 0 长到 196dp 时右端钉住，看上去像从
        // 铃铛那一侧原地铺开、往左把通知和重启两格吃掉。
        val endPad: Dp = if (state.anchorRightPx > 0f && hostLeftPx > 0f && hostWidthPx > 0f) {
            with(density) { (hostLeftPx + hostWidthPx - state.anchorRightPx).toDp() }
        } else {
            BAR_FALLBACK_END
        }
        val topPad: Dp = if (state.anchorCenterPx > 0f && hostTopPx > 0f) {
            with(density) { (state.anchorCenterPx - hostTopPx).toDp() } - BAR_HEIGHT / 2
        } else {
            8.dp
        }

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
                .padding(end = endPad, top = topPad),
            enter = expandHorizontally(
                expandFrom = Alignment.End,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 900f),
            ) + fadeIn(tween(durationMillis = 140)),
            exit = shrinkHorizontally(
                shrinkTowards = Alignment.End,
                animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
            ) + fadeOut(tween(durationMillis = 120)),
        ) {
            Row(
                modifier = Modifier
                    .width(BAR_WIDTH)
                    .height(BAR_HEIGHT)
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
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                REBOOT_OPTIONS.forEach { option ->
                    RebootBarItem(
                        option = option,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            state.dismiss()
                            onReboot(option.reason)
                        },
                    )
                }

                // 关闭格：压在原来那颗重启图标的位置，点它就是收起。
                Box(
                    modifier = Modifier
                        .width(CLOSE_SLOT_WIDTH)
                        .fillMaxHeight()
                        .clickable { state.dismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Close,
                        contentDescription = "收起",
                        tint = KuaiMaoColors.brand,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

/** 一格：16dp 图标 + 7sp 简称，塞进 26dp × 40dp。危险项走红色。 */
@Composable
private fun RebootBarItem(
    option: RebootOption,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (option.danger) KuaiMaoColors.danger else KuaiMaoColors.textLabel
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BAR_RADIUS - 4.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = option.short,
            color = if (option.danger) KuaiMaoColors.danger else KuaiMaoColors.textMuted,
            fontSize = 7.sp,
            lineHeight = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}
