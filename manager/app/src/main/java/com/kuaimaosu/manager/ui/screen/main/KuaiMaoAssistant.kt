package com.kuaimaosu.manager.ui.screen.main

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
import androidx.compose.material3.MaterialTheme
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
import com.kuaimaosu.manager.data.ai.AiSettingsStore
import kotlinx.coroutines.launch
import com.kuaimaosu.manager.ui.assistant.AssistantIntentParser
import com.kuaimaosu.manager.ui.assistant.KuaiMaoAiAgent
import com.kuaimaosu.manager.ui.assistant.KuaiMaoAssistantRuntime
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoGradients
import com.kuaimaosu.manager.ui.viewmodel.HomeUiState

private data class KuaiMaoChatMessage(
    val fromUser: Boolean,
    val text: String,
)

/**
 * KuaiMaoSU 内置助手。
 *
 * 命中本地动作（重启 / 跳转 / 安装）时走本地规则，结果取自当前设备的真实状态；
 * 其余问答在 AI 设置里配置了模型服务后交给远端模型，未配置或请求失败时回退本地规则。
 */
private val LOCAL_INTENT_WORDS = listOf(
    "重启", "关机", "恢复", "edl", "recovery", "bootloader", "reboot",
    "模块", "超级用户", "安装", "越狱",
    "主题", "字体", "通知", "关于", "设置", "日志", "仓库", "打开",
)

/** 一句话 → 一个页面跳转。命中后助手会直接打开对应界面，而不是回一句「请手动打开」。 */
private class KuaiMaoAction(val reply: String, val route: Route)

private fun kuaimaoNavigationIntent(query: String): KuaiMaoAction? {
    val q = query.lowercase()
    fun has(vararg words: String) = words.any { q.contains(it) }
    return when {
        has("主题设置", "换肤", "配色", "主题色", "背景特效", "字体") ->
            KuaiMaoAction("正在打开主题设置…外观、字体、背景特效都在这里。", Route.ThemeSettings)
        has("通知", "消息", "铃铛") ->
            KuaiMaoAction("正在打开通知页。", Route.Notifications)
        has("系统信息", "内核信息", "设备信息") ->
            KuaiMaoAction("正在打开系统信息。", Route.SystemInfo)
        has("模块仓库", "在线模块", "仓库") ->
            KuaiMaoAction("正在打开模块仓库。", Route.ModuleRepo)
        has("日志", "log") ->
            KuaiMaoAction("正在打开日志。", Route.Sulog)
        has("关于", "版本", "开源许可") ->
            KuaiMaoAction("正在打开关于页。", Route.About)
        has("设置") ->
            KuaiMaoAction("正在打开设置。", Route.Settings)
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuaiMaoAssistantSheet(
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
            KuaiMaoChatMessage(
                fromUser = false,
                text = "你好，我是 KuaiMaoSU 助手。当前${
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
    val navigator = LocalNavigator.current

    // 助手执行「打开页面 / 搜歌播放」时会往返回栈里推页面。
    // 这里盯一眼栈深：一旦推了新页就把面板收掉，否则面板会盖在新页面上，
    // 用户以为「说了半天什么都没打开」。
    val initialStackSize = remember { navigator.backStack.size }
    LaunchedEffect(navigator.backStack.size) {
        if (navigator.backStack.size != initialStackSize) onDismiss()
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        messages.add(KuaiMaoChatMessage(fromUser = true, text = trimmed))
        input = ""

        // 1) 明确的单步指令（换底栏、切质感、开页面、放歌、调音量）本地直接做，
        //    不走网络：没有配模型的用户也能用，配了的也不会白等一次请求。
        AssistantIntentParser.parse(trimmed)?.let { action ->
            val reply = KuaiMaoAssistantRuntime.execute(action)
            messages.add(KuaiMaoChatMessage(fromUser = false, text = reply))
            return
        }

        val localReply = kuaimaoReply(
            query = trimmed,
            uiState = uiState,
            onOpenSuperUser = onOpenSuperUser,
            onOpenModule = onOpenModule,
            onInstallModule = onInstallModule,
            onOpenSystemInfo = onOpenSystemInfo,
            onReboot = onReboot,
        )

        // 2) 破坏性动作（重启 / 安装 / 卸载）始终本地确认，不让模型替用户拍板。
        val destructive = listOf(
            "重启", "关机", "recovery", "bootloader", "edl", "安装", "卸载", "刷写", "刷入",
        ).any { trimmed.lowercase().contains(it) }
        if (destructive) {
            messages.add(KuaiMaoChatMessage(fromUser = false, text = localReply))
            return
        }

        // 3) 其余交给模型，并且允许它连续调用工具（打开页面、改设置、放歌）。
        val aiSettings = AiSettingsStore.load(context)
        if (!aiSettings.isConfigured) {
            messages.add(KuaiMaoChatMessage(fromUser = false, text = localReply))
            return
        }

        messages.add(KuaiMaoChatMessage(fromUser = false, text = "正在处理…"))
        val placeholderIndex = messages.lastIndex
        scope.launch {
            val history = messages
                .mapIndexedNotNull { index, message ->
                    if (index == placeholderIndex) null else message.fromUser to message.text
                }
            val result = KuaiMaoAiAgent.run(aiSettings, history) { step ->
                messages[placeholderIndex] = KuaiMaoChatMessage(fromUser = false, text = step)
            }
            val answer = result.getOrNull() ?: localReply
            messages[placeholderIndex] = KuaiMaoChatMessage(fromUser = false, text = answer)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KuaiMaoColors.canvasApp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(KuaiMaoColors.lineField),
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
                color = KuaiMaoColors.text,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "改设置 · 开页面 · 放音乐，直接说就行",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = KuaiMaoColors.textMuted,
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
                listOf("换成胶囊底栏", "切到模糊磨砂", "放首歌", "检查系统状态").forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(KuaiMaoColors.brandSoft)
                            .clickable { send(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        .background(if (recording) KuaiMaoColors.dangerSoft else KuaiMaoColors.canvasHover)
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
                        tint = if (recording) KuaiMaoColors.danger else KuaiMaoColors.brand,
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
                        Text("问点什么…", fontSize = 12.5.sp, color = KuaiMaoColors.textMuted)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.textBody,
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(input) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = KuaiMaoColors.canvasCardSolid,
                        unfocusedContainerColor = KuaiMaoColors.canvasCardSolid,
                        disabledContainerColor = KuaiMaoColors.canvasCardSolid,
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
                        .background(KuaiMaoGradients.button)
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
private fun ChatBubble(message: KuaiMaoChatMessage) {
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
                .background(if (message.fromUser) KuaiMaoColors.brand else KuaiMaoColors.canvasCard)
                .padding(horizontal = 13.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.text,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                color = if (message.fromUser) Color.White else KuaiMaoColors.textBody,
            )
        }
    }
}

/**
 * 本地规则应答。命中动作类问题时会直接执行（跳转 / 安装 / 重启），
 * 其余问题用当前设备状态作答。
 */
private fun kuaimaoReply(
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

    return "我可以帮你查状态、管模块、跳页面、执行重启。试试说「打开主题设置」「检查系统状态」「已安装哪些模块」或「重启到 Recovery」。"
}
