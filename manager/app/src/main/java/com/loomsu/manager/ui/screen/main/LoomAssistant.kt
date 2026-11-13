package com.loomsu.manager.ui.screen.main

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Send
import androidx.compose.material.icons.twotone.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loomsu.manager.data.ai.AiSettingsStore
import kotlinx.coroutines.launch
import com.loomsu.manager.data.ai.LoomAiClient
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomGradients
import com.loomsu.manager.ui.viewmodel.HomeUiState

private data class LoomChatMessage(
    val fromUser: Boolean,
    val text: String,
)

/**
 * Loom SU 内置助手。
 *
 * 命中本地动作（重启 / 跳转 / 安装）时走本地规则，结果取自当前设备的真实状态；
 * 其余问答在 AI 设置里配置了模型服务后交给远端模型，未配置或请求失败时回退本地规则。
 */
private val LOCAL_INTENT_WORDS = listOf(
    "重启", "关机", "恢复", "edl", "recovery", "bootloader", "reboot",
    "模块", "超级用户", "安装", "越狱",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoomAssistantSheet(
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onOpenSuperUser: () -> Unit,
    onOpenModule: () -> Unit,
    onInstallModule: () -> Unit,
    onOpenSystemInfo: () -> Unit,
    onReboot: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val messages = remember {
        mutableStateListOf(
            LoomChatMessage(
                fromUser = false,
                text = "你好，我是 Loom SU 助手。当前${
                    if (uiState.systemStatus.ksuVersion != null) "内核已加载" else "内核尚未激活"
                }，可以直接问我系统状态、模块或重启。",
            ),
        )
    }
    var input by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        recording = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) input = spoken
        }
    }

    val scope = rememberCoroutineScope()

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        messages.add(LoomChatMessage(fromUser = true, text = trimmed))
        input = ""

        val localReply = loomReply(
            query = trimmed,
            uiState = uiState,
            onOpenSuperUser = onOpenSuperUser,
            onOpenModule = onOpenModule,
            onInstallModule = onInstallModule,
            onOpenSystemInfo = onOpenSystemInfo,
            onReboot = onReboot,
        )

        // 命中本地动作（重启 / 跳转 / 安装）时以本地结果为准；
        // 其余问答在配置了模型服务后交给远端模型，失败自动回退本地应答。
        val localIntent = LOCAL_INTENT_WORDS.any { trimmed.lowercase().contains(it) }
        val aiSettings = AiSettingsStore.load(context)
        if (localIntent || !aiSettings.isConfigured) {
            messages.add(LoomChatMessage(fromUser = false, text = localReply))
            return
        }

        messages.add(LoomChatMessage(fromUser = false, text = "正在思考…"))
        val placeholderIndex = messages.lastIndex
        scope.launch {
            val result = LoomAiClient.chat(aiSettings, trimmed)
            val answer = result.getOrNull() ?: localReply
            messages[placeholderIndex] = LoomChatMessage(fromUser = false, text = answer)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LoomColors.canvasApp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(LoomColors.lineField),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "AI 助手",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = LoomColors.text,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "查状态 · 管模块 · 执行重启，直接说就行",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = LoomColors.textMuted,
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("检查系统状态", "安装模块", "重启到 Recovery").forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(LoomColors.brandSoft)
                            .clickable { send(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LoomColors.brandDeep,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (recording) LoomColors.dangerSoft else LoomColors.canvasHover)
                        .clickable {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "说出你的问题")
                            }
                            runCatching {
                                recording = true
                                speechLauncher.launch(intent)
                            }.onFailure {
                                recording = false
                                input = ""
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Mic,
                        contentDescription = "语音输入",
                        tint = if (recording) LoomColors.danger else LoomColors.brand,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp)),
                    placeholder = {
                        Text("问点什么…", fontSize = 12.5.sp, color = LoomColors.textMuted)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LoomColors.textBody,
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(input) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LoomColors.canvasCard,
                        unfocusedContainerColor = LoomColors.canvasCard,
                        disabledContainerColor = LoomColors.canvasCard,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(14.dp),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LoomGradients.button)
                        .clickable { send(input) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.TwoTone.Send,
                        contentDescription = "发送",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ChatBubble(message: LoomChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.fromUser) 16.dp else 5.dp,
                        bottomEnd = if (message.fromUser) 5.dp else 16.dp,
                    ),
                )
                .background(if (message.fromUser) LoomColors.brand else LoomColors.canvasCard)
                .padding(horizontal = 13.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.text,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                color = if (message.fromUser) Color.White else LoomColors.textBody,
            )
        }
    }
}

/**
 * 本地规则应答。命中动作类问题时会直接执行（跳转 / 安装 / 重启），
 * 其余问题用当前设备状态作答。
 */
private fun loomReply(
    query: String,
    uiState: HomeUiState,
    onOpenSuperUser: () -> Unit,
    onOpenModule: () -> Unit,
    onInstallModule: () -> Unit,
    onOpenSystemInfo: () -> Unit,
    onReboot: (String) -> Unit,
): String {
    val q = query.lowercase()
    val status = uiState.systemStatus
    val info = uiState.systemInfo

    val rebootHit = listOf("重启", "recovery", "bootloader", "关机", "edl", "reboot").any { q.contains(it) }
    if (rebootHit) {
        val target = when {
            q.contains("recovery") -> "recovery"
            q.contains("bootloader") -> "bootloader"
            q.contains("关机") -> "poweroff"
            q.contains("edl") -> "edl"
            else -> ""
        }
        onReboot(target)
        return if (target.isEmpty()) {
            "已发起完整重启。软重启、Recovery、Bootloader 等入口在底栏中央的深色按钮里。"
        } else {
            "已发起重启：$target。设备会在几秒内断开，属正常现象。"
        }
    }

    if (listOf("安装", "模块", "zip").any { q.contains(it) }) {
        if (q.contains("查看") || q.contains("管理")) {
            onOpenModule()
            return "已打开模块页。当前已安装 ${info.moduleCount} 个模块。"
        }
        onInstallModule()
        return "已打开安装向导，可以选择本地 zip 或镜像。当前已安装 ${info.moduleCount} 个模块，安装后需要重启生效。"
    }

    if (listOf("超级用户", "授权", "root 列表", "su").any { q.contains(it) }) {
        onOpenSuperUser()
        return "已打开超级用户页，当前有 ${info.superuserCount} 个应用获得授权。"
    }

    if (listOf("状态", "检查", "健康", "信息", "详情").any { q.contains(it) }) {
        val seccomp = when (info.seccompStatus) {
            -1 -> "不支持"
            0 -> "已禁用"
            1 -> "严格"
            2 -> "过滤"
            else -> "未知"
        }
        return buildString {
            append("内核 ${info.kernelRelease.ifBlank { "未知" }}，")
            append(if (status.ksuVersion != null) "Root 已激活" else "Root 未激活")
            append("。SELinux ${info.selinuxStatus.ifBlank { "未知" }}，Seccomp $seccomp，")
            append("超级用户 ${info.superuserCount} 个、模块 ${info.moduleCount} 个。")
            if (status.isSELinuxPermissive) append("注意：SELinux 处于宽容模式，隐藏能力会下降。")
        }
    }

    if (listOf("selinux", "安全").any { q.contains(it) }) {
        return "SELinux 当前为 ${info.selinuxStatus.ifBlank { "未知" }}。${
            if (status.isSELinuxPermissive) "建议切回强制模式以恢复完整隐藏能力。" else "安全上下文完整。"
        }"
    }

    return "我可以帮你查状态、管模块、执行重启。试试说「检查系统状态」「已安装哪些模块」或「重启到 Recovery」。"
}
