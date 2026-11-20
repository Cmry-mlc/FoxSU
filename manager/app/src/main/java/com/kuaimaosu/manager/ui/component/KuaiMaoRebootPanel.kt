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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors

/**
 * 重启条的展开状态与回调。
 *
 * 状态放在 MainScreen 上（四个页签共用一条），标题栏里的重启按钮只负责 flip
 * 这个开关；真正的重启动作由 MainScreen 塞进 [onReboot]，因为那层才知道
 * 当前有没有拿到 root、要不要提示。
 */
class RebootPanelState {
    var expanded by mutableStateOf(false)
        private set

    /** 选中某项后要做的事，由 MainScreen 注入。 */
    var onReboot: ((String) -> Unit)? = null

    fun toggle() {
        expanded = !expanded
    }

    fun dismiss() {
        expanded = false
    }

    /** 收起并执行动作：先收再执行，避免面板挂在还没重启完的界面上。 */
    fun trigger(reason: String) {
        dismiss()
        onReboot?.invoke(reason)
    }
}

val LocalRebootPanel = staticCompositionLocalOf { RebootPanelState() }

/**
 * 展开后的整条宽度。
 *
 * 收起时标题栏右侧只占「铃铛 + 重启」两格（80dp），展开后这条直接把它们盖掉。
 * 宽度按实测取的：标题「KuaiMaoSU」右边界到动作条左边缘还有 45dp 空地，把条
 * 从 196dp 拉到 228dp，左边仍留约 10dp 呼吸位，不会碰到标题最后一个字。
 * 六格各 32dp、标签 8sp，比原来宽了一档，字也清楚一档。
 */
private val BAR_WIDTH = 228.dp

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
 * 重启条：展开时原地盖住标题栏右侧的「铃铛 + 重启」，换成六个动作 + 一个关闭格。
 *
 * **必须放在标题栏那一层的 Box 里**（各页标题行外层包一个 `fillMaxWidth` 的 Box，
 * 用 `Modifier.align(Alignment.CenterEnd)` 摆进来），不要再包 Popup。
 *
 * 之前两版都错在「让这条脱离父布局」：Popup 是独立窗口，位置要靠窗口坐标换算，
 * 一旦页面被状态栏内边距顶下去，整条就往上错一行；更糟的是分页容器会预组合相邻
 * 页，而设置页标题栏也挂了同一份面板状态，于是**两个窗口同时显示**——屏幕上看得
 * 见两条「重启」，还互相错开一格。
 *
 * 现在它就在标题栏这一层里当兄弟节点画：右边缘天生和动作条右边缘齐平，垂直跟着
 * 标题行居中，坐标一次都不用手算，也不会跨页重复出现（离屏的页被分页容器裁掉）。
 */
@Composable
fun KuaiMaoRebootBar(
    modifier: Modifier = Modifier,
    state: RebootPanelState = LocalRebootPanel.current,
) {
    if (!state.expanded) return

    val shape = RoundedCornerShape(BAR_RADIUS)

    // 入场只做「从右端往左摊开」：缩放不改变测量尺寸，右端在整个动画里钉死不动。
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scaleX by animateFloatAsState(
        targetValue = if (entered) 1f else 0.02f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 900f),
        label = "rebootBarScaleX",
    )

    Row(
        modifier = modifier
            .width(BAR_WIDTH)
            .height(BAR_HEIGHT)
            .graphicsLayer {
                this.scaleX = scaleX
                transformOrigin = TransformOrigin(1f, 0.5f)
            }
            .clip(shape)
            // 实色卡片：它是标题栏上的覆盖层，跟着标题行的底色走，不用背景模糊。
            .background(KuaiMaoColors.canvasCard)
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
                onClick = { state.trigger(option.reason) },
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
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 一格：17dp 图标 + 8sp 简称，塞进 32dp × 40dp。危险项走红色。 */
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
            modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = option.short,
            color = if (option.danger) KuaiMaoColors.danger else KuaiMaoColors.textMuted,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}
