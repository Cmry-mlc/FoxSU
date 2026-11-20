package com.kuaimaosu.manager.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import org.koin.compose.koinInject
import kotlin.math.roundToInt

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
     * 标题栏动作条在窗口里的矩形（px）。
     *
     * 条要原地盖住这块（铃铛 + 重启），所以位置必须来自实测、不能用父布局换算：
     * 首页是 44dp 头像行、其余页是 24sp 大标题 + 副标题，各页高度不一样，任何
     * 「写死的内边距 + 偏移」都会在某一页错位。这里直接拿窗口矩形，交给 Popup
     * 的定位器用绝对坐标摆放，不参与父布局，也没有坐标系换算。
     */
    var anchorBounds by mutableStateOf<IntRect?>(null)

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
 * 把条摆在锚点矩形上：右端与锚点右端齐平，垂直方向与锚点同高居中。
 *
 * 用 Popup 而不是在页面里叠一层，是因为页面里的层要自己做「窗口坐标 → 本层坐标」
 * 的换算，一旦这一层和标题栏不在同一个 inset 基准上（比如页面被状态栏内边距顶下去
 * 了、根 Box 却是全屏的），就会整体错开一个状态栏的高度——条会跑到状态栏里去。
 * Popup 直接在窗口坐标系里摆，锚点是实测的窗口矩形，不存在换算，也就不会错位。
 */
private class AnchorRightPositionProvider(
    private val anchor: IntRect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchor.right - popupContentSize.width
        val y = anchor.top + (anchor.height - popupContentSize.height) / 2
        return IntOffset(
            x = x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y = y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

/**
 * 重启条：展开时原地盖住标题栏右侧的「铃铛 + 重启」，换成六个动作 + 一个关闭格。
 *
 * 为什么覆盖而不是另找地方放：标题栏中间只剩 137dp 的窄缝，六格挤进去每格只有
 * 21dp、文字得压到 6sp，基本看不清；而铃铛 + 重启本身占着 80dp。展开时把这块
 * 让出来，整条可用宽度就有约 200dp，每格 26dp、文字 7sp，不用牺牲可读性。
 *
 * 关闭格钉在最右端、和原来那颗重启图标同一个位置：用户想「再点一下关掉」时，
 * 手指落点就是关闭，不会误触到关机或 EDL。
 */
@Composable
fun KuaiMaoRebootPanel(
    state: RebootPanelState,
    onReboot: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anchor = state.anchorBounds ?: return
    if (!state.expanded) return

    val cardConfig: CardConfig = koinInject()
    val onWallpaper = cardConfig.isCustomBackgroundEnabled
    val shape = RoundedCornerShape(BAR_RADIUS)

    // Popup 是立刻出现的，所以入场只做「从右端往左摊开」：缩放不改变测量尺寸，
    // 定位器算出的位置在整个动画里都不变，右端钉死，不会一边长一边漂。
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scaleX by animateFloatAsState(
        targetValue = if (entered) 1f else 0.02f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 900f),
        label = "rebootBarScaleX",
    )

    Popup(
        popupPositionProvider = remember(anchor) { AnchorRightPositionProvider(anchor) },
        onDismissRequest = { state.dismiss() },
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
    ) {
        Row(
            modifier = modifier
                .width(BAR_WIDTH)
                .height(BAR_HEIGHT)
                .graphicsLayer {
                    this.scaleX = scaleX
                    transformOrigin = TransformOrigin(1f, 0.5f)
                }
                .clip(shape)
                // Popup 是独立窗口，拿不到页面里的 blurSource，所以这里不用背景模糊，
                // 改用实色/半透明实色：壁纸下压一层 92% 的白或黑，图标依然清楚。
                .background(
                    if (onWallpaper) {
                        KuaiMaoColors.canvasCard.copy(alpha = 0.92f)
                    } else {
                        KuaiMaoColors.canvasCard
                    },
                )
                .border(
                    width = 1.dp,
                    color = KuaiMaoColors.line.copy(alpha = 0.8f),
                    shape = shape,
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

/** 把窗口里的浮点矩形收成整数矩形，供 Popup 定位使用。 */
internal fun androidx.compose.ui.geometry.Rect.toWindowIntRect(): IntRect = IntRect(
    left = left.roundToInt(),
    top = top.roundToInt(),
    right = right.roundToInt(),
    bottom = bottom.roundToInt(),
)

/** 采集锚点矩形的小工具：给标题栏动作条挂一次即可。 */
internal fun Modifier.rebootAnchor(
    state: RebootPanelState,
): Modifier = onGloballyPositioned {
    state.anchorBounds = it.boundsInWindow().toWindowIntRect()
}
