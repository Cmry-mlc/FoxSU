package com.kuaimaosu.manager.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.renderBackgroundBlur
import org.koin.compose.koinInject

/**
 * 电源与重启面板。
 *
 * 这里是**主 composition 内的浮层**，不是 ModalBottomSheet：
 * 弹窗自带一个独立窗口，[renderBackgroundBlur] 靠 `localToScreen` 往背景层上对位，
 * 跨窗口后算出来的矩形是错的，于是磨砂画不出来，面板就只剩一层半透明底色，
 * 看着像「透明纸」。放在同一窗口里，磨砂才能真的贴着壁纸采样。
 *
 * 高度按三个一块、两行排布，去掉三行六宫格那一版多出来的 1/3 屏。
 */
@Composable
fun RebootSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onReboot: (String) -> Unit,
) {
    // 0 = 完全收起，1 = 完全展开。入场带一点点回弹，退场只是快速下滑。
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 520f))
        } else {
            progress.animateTo(0f, tween(durationMillis = 180, easing = FastOutLinearInEasing))
        }
    }
    // 收起动画跑完才真正从树上摘掉，避免「一关就消失」。
    if (!visible && progress.value <= 0.001f) return

    val themeConfig: ThemeConfig = koinInject()
    // 跟设置里的「启用模糊」总开关 + 「绘制背景以进行模糊」子开关联动：
    // 两个都开才走磨砂，否则退回完全实色的面板，绝不让背后的内容透上来。
    val blurEnabled = themeConfig.isEnableBlur && themeConfig.isEnableBlurExp
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val scrimInteraction = remember { MutableInteractionSource() }

    val actions = listOf(
        RebootAction("重启", "完整重启", "reboot", Icons.TwoTone.RestartAlt),
        RebootAction("软重启", "仅系统界面", "soft_reboot", Icons.TwoTone.Refresh),
        RebootAction("Recovery", "恢复模式", "recovery", Icons.TwoTone.SettingsBackupRestore),
        RebootAction("Bootloader", "引导加载器", "bootloader", Icons.TwoTone.DeveloperBoard),
        RebootAction("EDL", "紧急下载", "edl", Icons.TwoTone.Usb, danger = true),
        RebootAction("关机", "关闭电源", "poweroff", Icons.TwoTone.PowerSettingsNew, danger = true),
    )

    BackHandler(enabled = visible) { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩：只压暗，不加额外颜色，避免和磨砂叠成一层灰。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * 0.38f }
                .background(Color.Black)
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                ) { onDismiss() },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    // 从屏幕下方推上来；只改图层位移，不参与重组。
                    translationY = (1f - progress.value) * size.height
                }
                .clip(sheetShape)
                .background(
                    if (blurEnabled) KuaiMaoColors.canvasCard else KuaiMaoColors.canvasCardSolid,
                )
                .then(
                    if (blurEnabled) {
                        Modifier.renderBackgroundBlur(
                            tintColor = KuaiMaoColors.canvasCardSolid,
                            // 0.80 的膜：字仍然清清楚楚，背后的壁纸被压成一片模糊色块。
                            tintAlpha = 0.80f,
                        )
                    } else {
                        Modifier
                    },
                )
                // 面板自己吃掉点击，否则点在空白处会冒泡到遮罩上直接关掉。
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {}
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(top = 8.dp, bottom = 18.dp),
        ) {
            // grab 条
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(KuaiMaoColors.lineField),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "电源",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.2).sp,
                    color = KuaiMaoColors.text,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "重启或关机",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = KuaiMaoColors.textMuted,
                )
                Spacer(modifier = Modifier.weight(1f))
                // 关闭按钮：和面板里的 tile 同一套圆角与底色，但它是圆的。
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(KuaiMaoColors.canvasField)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Close,
                        contentDescription = "关闭",
                        tint = KuaiMaoColors.textMuted,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            actions.chunked(3).forEachIndexed { index, row ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { action ->
                        RebootTile(
                            action = action,
                            modifier = Modifier.weight(1f),
                            onClick = { onReboot(action.reason) },
                        )
                    }
                }
            }
        }
    }
}

private data class RebootAction(
    val label: String,
    val desc: String,
    val reason: String,
    val icon: ImageVector,
    val danger: Boolean = false,
)

/**
 * 一格里只有三行：图标、标题、一句短说明。
 * 图标放进 10% 的色块里，危险项（EDL / 关机）整格转红，和其余四项区分开。
 */
@Composable
private fun RebootTile(
    action: RebootAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val accent = if (action.danger) KuaiMaoColors.danger else KuaiMaoColors.brand
    Column(
        modifier = modifier
            .clip(shape)
            .background(KuaiMaoColors.canvasField)
            .border(1.dp, KuaiMaoColors.line, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = action.label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Black,
            color = if (action.danger) KuaiMaoColors.danger else KuaiMaoColors.textBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = action.desc,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = KuaiMaoColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
