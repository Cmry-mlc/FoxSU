package me.weishu.kernelsu.ui.screen.ag

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.component.rebootlistpopup.getRebootListOption
import me.weishu.kernelsu.ui.component.rebootlistpopup.rememberRebootAction

/**
 * 重启菜单（设计稿 .mask + .sheet）：毛玻璃 Bottom Sheet，
 * 六项：重启 / 软重启 / Recovery / Bootloader / Download / EDL（红色警示）。
 */
@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun AgRebootSheet(onDismiss: () -> Unit) {
    val options = getRebootListOption()
    val reboot = rememberRebootAction()
    // options 顺序即 getRebootListOption 返回顺序；EDL 恒为最后一项且为红色警示
    fun subtitleFor(reason: String): String = when (reason) {
        "" -> "完整重启设备"
        "soft_reboot" -> "仅重启系统界面（Soft Reboot）"
        "recovery" -> "进入 Recovery 模式"
        "bootloader" -> "进入 Fastboot 模式"
        "download" -> "进入 Oxford/Download 模式"
        "edl" -> "进入紧急下载模式（9008）"
        else -> ""
    }
    val icons = listOf(
        Icons.Rounded.PowerSettingsNew,
        Icons.Rounded.Bolt,
        Icons.Rounded.Refresh,
        Icons.Rounded.SystemUpdate,
        Icons.Rounded.RestartAlt,
        Icons.Rounded.Error,
    )

    // 注意：必须在 Dialog 之外读取，暗色状态由宿主 composition 提供
    // 设计稿 .mask/.sheet：mask rgba(25,20,60,.28)；sheet 亮 rgba(255,255,255,.85)/描边 rgba(255,255,255,.65)，
    // 暗 rgba(28,27,46,.88)/描边 rgba(255,255,255,.1)；定位 top 118 / right 14 / 宽 230 / 圆角 22
    val dark = Ag.isDark
    // 面板必须近乎不透明：原 0.85 会让下方状态卡/按钮透出，形成一条"白色横条"的错觉
    val panel = if (dark) Color(0xFA1C1B2E) else Color(0xFAFFFFFF)
    val panelLine = if (dark) Color(0x1FFFFFFF) else Color(0xB3FFFFFF)
    val iconBg = Color(0x1A6C5CE7)
    val mask = Color(0x4719143C)
    val tx = if (dark) Color(0xFFF2F1FA) else Color(0xFF191826)
    val tx3 = if (dark) Color(0xFF7C7A98) else Color(0xFF9C9AAE)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
    // Dialog 自带的白底 + 默认 dim 会露出白色硬边，这里强制透明窗口
    val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
    androidx.compose.runtime.SideEffect {
        dialogWindow?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
        dialogWindow?.setDimAmount(0f)
    }
    Box(Modifier.fillMaxSize()) {
        // 遮罩
        Box(
            Modifier
                .fillMaxSize()
                .background(mask)
                .agClick(onClick = onDismiss),
        )
        Column(
            // 设计稿 .sheet：锚定电源按钮的下拉菜单（右上角，宽 230，圆角 22）
            // 顺序：shadow → clip → background → border；阴影放后面会把光晕盖到面板上
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 118.dp, end = 14.dp)
                .width(230.dp)
                .shadow(
                    18.dp, RoundedCornerShape(22.dp),
                    spotColor = Color(0x40281E64),
                    ambientColor = Color(0x1A281E64),
                )
                .clip(RoundedCornerShape(22.dp))
                .background(panel)
                .border(1.dp, panelLine, RoundedCornerShape(22.dp))
                .padding(8.dp),
        ) {
            options.forEachIndexed { i, opt ->
                val warn = opt.reason == "edl"
                val tint = if (warn) Ag.Danger else Ag.Vio
                Row(
                    Modifier
                        .fillMaxWidth()
                        .agClick {
                            onDismiss()
                            reboot(opt.reason)
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .padding(10.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (warn) Color(0x17DC2626) else iconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icons.getOrElse(i) { Icons.Rounded.Refresh }, null, tint = tint, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(stringResource(opt.labelRes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (warn) Ag.Danger else tx)
                        Spacer(Modifier.height(1.dp))
                        Text(subtitleFor(opt.reason), fontSize = 10.5.sp, color = tx3)
                    }
                }
            }
        }
    }
}
}
