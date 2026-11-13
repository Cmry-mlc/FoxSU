package com.loomsu.manager.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material.icons.twotone.Mic
import androidx.compose.material.icons.twotone.VerifiedUser
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loomsu.manager.data.ai.AiProvider
import com.loomsu.manager.data.ai.AiProviders
import com.loomsu.manager.data.ai.AiSettings
import com.loomsu.manager.data.ai.AiSettingsStore
import com.loomsu.manager.data.ai.AiTestReport
import com.loomsu.manager.data.ai.LoomAiClient
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomRowDivider
import com.loomsu.manager.ui.component.LoomFieldLabel
import com.loomsu.manager.ui.component.LoomGhostButton
import com.loomsu.manager.ui.component.LoomPrimaryButton
import com.loomsu.manager.ui.component.LoomTextField
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.LoomTones
import com.loomsu.manager.ui.theme.LoomType
import com.loomsu.manager.ui.util.LocalSnackbarHost
import com.loomsu.manager.ui.util.showReplacingSnackbar
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * AI 设置（设计稿 pages/aiset）。
 *
 * 配置真实落盘到应用私有 SharedPreferences，AI 助手会读取同一份配置；
 * 未配置 Key 时助手自动回退到本地规则应答。
 */
@Composable
fun SettingsAiPage(bottomPadding: Dp = 0.dp) {
    val context = LocalContext.current
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(AiSettingsStore.load(context)) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<AiTestReport?>(null) }
    var testError by remember { mutableStateOf<String?>(null) }

    fun update(transform: (AiSettings) -> AiSettings) {
        settings = transform(settings)
        AiSettingsStore.save(context, settings)
    }

    LoomSettingsScaffold(
        title = "AI 设置",
        subtitle = "自定义模型与服务连接，配置后 AI 助手将使用此模型",
        bottomPadding = bottomPadding,
    ) {
        item {
            Text(
                text = "模型服务",
                style = LoomType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
            )
        }

        item {
            LoomCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
                    Text(
                        text = "提供方",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = LoomColors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProviderGrid(
                        providers = AiProviders.all,
                        selectedId = settings.providerId,
                        onSelect = { provider ->
                            update {
                                it.copy(
                                    providerId = provider.id,
                                    baseUrl = provider.baseUrl.ifBlank { it.baseUrl },
                                    model = provider.model.ifBlank { it.model },
                                )
                            }
                        },
                    )

                    LoomFieldLabel(text = "API 地址")
                    LoomTextField(
                        value = settings.baseUrl,
                        onValueChange = { value -> update { it.copy(baseUrl = value) } },
                        placeholder = "https://api.openai.com/v1",
                        keyboardType = KeyboardType.Uri,
                    )

                    LoomFieldLabel(text = "API Key")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LoomTextField(
                            value = settings.apiKey,
                            onValueChange = { value ->
                                update { it.copy(apiKey = value) }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = "sk-…",
                            isPassword = !apiKeyVisible,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(LoomColors.canvasField)
                                .clickable { apiKeyVisible = !apiKeyVisible }
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                        ) {
                            Text(
                                text = if (apiKeyVisible) "隐藏" else "显示",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = LoomColors.textSec,
                            )
                        }
                    }

                    LoomFieldLabel(text = "模型名称")
                    LoomTextField(
                        value = settings.model,
                        onValueChange = { value -> update { it.copy(model = value) } },
                        placeholder = "gpt-4o-mini",
                    )
                }
            }
        }

        item {
            Text(
                text = "助手行为",
                style = LoomType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
            )
        }

        item {
            LoomCard {
                LoomSwitchRow(
                    icon = Icons.TwoTone.Language,
                    title = "联网搜索",
                    description = "允许助手访问网络获取最新信息",
                    checked = settings.webSearch,
                    tone = LoomTones.blue,
                    onCheckedChange = { value -> update { it.copy(webSearch = value) } },
                )
                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.Mic,
                    title = "语音输入",
                    description = "在对话中显示麦克风，长按说话",
                    checked = settings.voiceInput,
                    tone = LoomTones.violet,
                    onCheckedChange = { value -> update { it.copy(voiceInput = value) } },
                )
                LoomRowDivider()
                LoomSwitchRow(
                    icon = Icons.TwoTone.VerifiedUser,
                    title = "执行前确认",
                    description = "涉及重启、卸载等操作时先询问",
                    checked = settings.confirmBeforeRun,
                    tone = LoomTones.green,
                    onCheckedChange = { value -> update { it.copy(confirmBeforeRun = value) } },
                )
            }
        }

        item {
            Text(
                text = "生成参数",
                style = LoomType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
            )
        }

        item {
            LoomCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                SliderRow(
                    label = "温度（创造性）",
                    valueText = String.format("%.1f", settings.temperature),
                ) {
                    Slider(
                        value = settings.temperature,
                        onValueChange = { value ->
                            settings = settings.copy(temperature = value)
                        },
                        onValueChangeFinished = {
                            AiSettingsStore.save(context, settings)
                        },
                        valueRange = 0f..2f,
                        steps = 19,
                        colors = loomSliderColors(),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SliderRow(
                    label = "最大回复长度",
                    valueText = settings.maxTokens.toString(),
                ) {
                    Slider(
                        value = settings.maxTokens.toFloat(),
                        onValueChange = { value ->
                            settings = settings.copy(
                                maxTokens = (value / 256f).roundToInt() * 256,
                            )
                        },
                        onValueChangeFinished = {
                            AiSettingsStore.save(context, settings)
                        },
                        valueRange = 256f..8192f,
                        colors = loomSliderColors(),
                    )
                }

                LoomFieldLabel(text = "系统提示词")
                LoomTextField(
                    value = settings.systemPrompt,
                    onValueChange = { value -> update { it.copy(systemPrompt = value) } },
                    singleLine = false,
                    minLines = 3,
                )
            }
        }

        item {
            LoomPrimaryButton(
                text = if (testing) "测速中…" else "测试连接",
                enabled = !testing && settings.baseUrl.isNotBlank(),
                modifier = Modifier.padding(top = 18.dp),
                onClick = {
                    if (testing) return@LoomPrimaryButton
                    testing = true
                    report = null
                    testError = null
                    scope.launch {
                        // 真实测速：连接耗时、整轮延迟、服务端 token 统计都来自这次请求
                        val result = LoomAiClient.measure(settings)
                        testing = false
                        result.fold(
                            onSuccess = { report = it },
                            onFailure = {
                                testError = it.message ?: "未知错误"
                                snackBarHost.showReplacingSnackbar("连接失败：${it.message ?: "未知错误"}")
                            },
                        )
                    }
                },
            )

            report?.let { data ->
                Spacer(modifier = Modifier.height(10.dp))
                LoomCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "实测结果",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = LoomColors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricCell(
                            label = "响应延迟",
                            value = formatMs(data.latencyMs),
                            tone = LoomColors.success,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCell(
                            label = "生成速度",
                            value = String.format("%.1f tok/s", data.tokensPerSecond),
                            tone = LoomColors.brand,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCell(
                            label = "连接耗时",
                            value = formatMs(data.connectMs),
                            tone = LoomColors.textLabel,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "模型 ${data.model} · 输出 ${data.completionTokens} tokens" +
                            if (data.promptTokens > 0) " · 输入 ${data.promptTokens} tokens" else "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LoomColors.textMuted,
                    )
                }
            }

            testError?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                LoomCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "连接失败",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = LoomColors.danger,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = LoomColors.textSec,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LoomGhostButton(
                text = "清除密钥",
                onClick = {
                    AiSettingsStore.clearKey(context)
                    settings = settings.copy(apiKey = "")
                    scope.launch { snackBarHost.showReplacingSnackbar("已清除密钥") }
                },
            )
        }

        item {
            Text(
                text = "密钥仅保存在本机应用私有存储中，不会上传。",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = LoomColors.textMuted,
                modifier = Modifier.padding(start = 2.dp, top = 14.dp),
            )
        }
    }
}

@Composable
private fun ProviderGrid(
    providers: List<AiProvider>,
    selectedId: String,
    onSelect: (AiProvider) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        providers.chunked(3).forEach { rowProviders ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowProviders.forEach { provider ->
                    val selected = provider.id == selectedId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) {
                                    Brush.linearGradient(
                                        colors = listOf(LoomColors.brand, LoomColors.brandDeep),
                                        start = Offset.Zero,
                                        end = Offset.Infinite,
                                    )
                                } else {
                                    SolidColor(LoomColors.canvasField)
                                },
                            )
                            .clickable { onSelect(provider) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = provider.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (selected) Color.White else LoomColors.textSec,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                repeat(3 - rowProviders.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LoomColors.textLabel,
            )
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = LoomColors.brand,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun loomSliderColors() = SliderDefaults.colors(
    thumbColor = LoomColors.brand,
    activeTrackColor = LoomColors.brand,
    inactiveTrackColor = LoomColors.canvasSubtle,
)


/** 千毫秒以上转成秒，保持两位有效数字，避免长串数字挤爆卡片。 */
private fun formatMs(ms: Long): String =
    if (ms >= 1000) String.format("%.2f s", ms / 1000.0) else "$ms ms"

@Composable
private fun MetricCell(
    label: String,
    value: String,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LoomColors.canvasField)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = tone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = LoomColors.textMuted,
            maxLines = 1,
        )
    }
}
