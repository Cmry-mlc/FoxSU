package com.kuaimaosu.manager.ui.screen.zygisk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Bolt
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.data.shell.KsuCliRepository
import com.kuaimaosu.manager.data.zygisk.ZygiskRepository
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.component.KuaiMaoSwitch
import com.kuaimaosu.manager.ui.component.rememberLoadingDialog
import com.kuaimaosu.manager.ui.screen.main.KuaiMaoSettingsScaffold
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
import org.koin.compose.koinInject

/**
 * 内置 Zygisk 引擎控制页。
 *
 * 引擎直接编进 ksud、随管理器分发，无需安装任何 Zygisk 模块。
 * 检测到外部 provider（ZygiskNext / ReZygisk 等）时内置引擎自动让位。
 */
@Composable
fun ZygiskScreen(bottomPadding: Dp = 0.dp) {
    val repository = koinInject<ZygiskRepository>()
    val ksuCli = koinInject<KsuCliRepository>()
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val loadingDialog = rememberLoadingDialog()

    var state by remember { mutableStateOf(ZygiskRepository.Status()) }
    var busy by remember { mutableStateOf(false) }

    suspend fun refresh() {
        state = repository.status()
    }

    LaunchedEffect(Unit) { refresh() }

    fun runAction(
        successMessage: String,
        block: suspend () -> Result<String>,
    ) {
        if (busy) return
        busy = true
        scope.launch {
            loadingDialog.show()
            val result = block()
            loadingDialog.hide()
            busy = false
            refresh()
            snackbarHost.showReplacingSnackbar(
                result.getOrElse { it.message ?: "操作失败" }.lineSequence().firstOrNull().orEmpty()
                    .ifBlank { successMessage },
            )
        }
    }

    val headline = when {
        !state.supported -> "当前内核组件不支持"
        state.yieldedToExternal -> "已让位外部引擎"
        state.enabled && state.injected -> "运行中"
        state.enabled && state.rebootRequired -> "待重启生效"
        state.enabled -> "已启用"
        else -> "未启用"
    }
    val description = when {
        !state.supported -> "请更新管理器与内置组件后重试"
        state.yieldedToExternal -> "检测到外部 Zygisk 提供者：${state.externalProviders.joinToString("、")}，内置引擎已自动让位"
        state.enabled && state.injected -> "内置 Zygisk 已注入 zygote，Zygisk 模块可正常使用"
        state.enabled && state.rebootRequired -> "配置已保存，重启后引擎将注入 zygote"
        state.enabled -> "已启用，下一次启动时引擎会注入 zygote"
        else -> "开启后无需安装模块，重启即可使用 Zygisk"
    }
    val statusActive = state.supported && state.enabled && !state.yieldedToExternal

    KuaiMaoSettingsScaffold(
        title = "内置Zygisk",
        subtitle = "无需模块的 Zygisk",
        bottomPadding = bottomPadding,
    ) {
        item {
            KuaiMaoCard {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Bolt,
                    title = "引擎状态",
                    description = description,
                    iconTint = if (statusActive) KuaiMaoTones.green.content else KuaiMaoTones.slate.content,
                    iconContainer = if (statusActive) KuaiMaoTones.green.container else KuaiMaoTones.slate.container,
                    trailing = {
                        StatusBadge(
                            text = headline,
                            active = statusActive,
                            warn = state.rebootRequired || state.yieldedToExternal,
                        )
                    },
                )
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                KuaiMaoRow(
                    icon = Icons.TwoTone.VerifiedUser,
                    title = "启用内置引擎",
                    description = "关闭后下一次启动不再注入；不会改动已安装的模块",
                    enabled = !busy && state.supported && !state.yieldedToExternal,
                    iconTint = KuaiMaoTones.violet.content,
                    iconContainer = KuaiMaoTones.violet.container,
                    trailing = {
                        KuaiMaoSwitch(
                            checked = state.enabled,
                            enabled = !busy && state.supported && !state.yieldedToExternal,
                            onCheckedChange = { enabled ->
                                runAction(if (enabled) "已启用，重启后生效" else "已关闭，重启后生效") {
                                    if (enabled) repository.enable() else repository.disable()
                                }
                            },
                        )
                    },
                )
            }
        }

        item {
            KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                KuaiMaoRow(
                    icon = Icons.TwoTone.Extension,
                    title = "外部引擎",
                    description = if (state.yieldedToExternal) {
                        state.externalProviders.joinToString("、")
                    } else {
                        "未检测到外部 Zygisk 提供者，内置引擎接管"
                    },
                    iconTint = KuaiMaoTones.blue.content,
                    iconContainer = KuaiMaoTones.blue.container,
                    trailing = {
                        StatusBadge(
                            text = if (state.yieldedToExternal) "外部优先" else "无",
                            active = state.yieldedToExternal,
                        )
                    },
                )
                KuaiMaoRowDivider()
                KuaiMaoRow(
                    icon = Icons.TwoTone.Info,
                    title = "注入状态",
                    description = injectionLabel(state.injectionState),
                    iconTint = KuaiMaoTones.amber.content,
                    iconContainer = KuaiMaoTones.amber.container,
                )
            }
        }

        if (state.rebootRequired && state.supported && !state.yieldedToExternal) {
            item {
                KuaiMaoCard(modifier = Modifier.padding(top = 12.dp)) {
                    KuaiMaoRow(
                        icon = Icons.TwoTone.RestartAlt,
                        title = "重启以生效",
                        description = "引擎已就绪，重启后开始注入 zygote",
                        iconTint = KuaiMaoTones.rose.content,
                        iconContainer = KuaiMaoTones.rose.container,
                        enabled = !busy,
                        onClick = { ksuCli.reboot() },
                        trailing = {
                            Icon(
                                imageVector = Icons.TwoTone.ChevronRight,
                                contentDescription = null,
                                tint = KuaiMaoColors.chevron,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "内置 Zygisk 引擎随管理器一同分发，无需单独安装模块。" +
                    "当系统里装有外部 Zygisk 提供者时，内置引擎会自动让位，避免冲突；" +
                    "移除外部提供者后，下一次启动会重新接管。开关均在重启后生效。",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

private fun injectionLabel(state: String): String = when (state) {
    "injected" -> "已注入 zygote"
    "partial" -> "部分架构已注入"
    "starting" -> "正在注入"
    "failed" -> "注入失败（可尝试重启）"
    "unverified" -> "本次启动尚未验证"
    else -> state
}

@Composable
private fun StatusBadge(text: String, active: Boolean, warn: Boolean = false) {
    val tone = when {
        active -> KuaiMaoTones.green
        warn -> KuaiMaoTones.amber
        else -> null
    }
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = tone?.content ?: KuaiMaoColors.textMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .then(
                if (tone != null) Modifier.background(tone.container) else Modifier,
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}
