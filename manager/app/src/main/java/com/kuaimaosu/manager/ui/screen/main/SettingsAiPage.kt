package com.kuaimaosu.manager.ui.screen.main

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
import com.kuaimaosu.manager.data.ai.AiProvider
import com.kuaimaosu.manager.data.ai.AiProviders
import com.kuaimaosu.manager.data.ai.AiSettings
import com.kuaimaosu.manager.data.ai.AiSettingsStore
import com.kuaimaosu.manager.data.ai.AiTestReport
import com.kuaimaosu.manager.data.ai.KuaiMaoAiClient
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.component.KuaiMaoFieldLabel
import com.kuaimaosu.manager.ui.component.KuaiMaoGhostButton
import com.kuaimaosu.manager.ui.component.KuaiMaoPrimaryButton
import com.kuaimaosu.manager.ui.component.KuaiMaoTextField
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones
import com.kuaimaosu.manager.ui.theme.KuaiMaoType
import com.kuaimaosu.manager.ui.util.LocalSnackbarHost
import com.kuaimaosu.manager.ui.util.showReplacingSnackbar
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

    KuaiMaoSettingsScaffold(
        title = "AI 设置",
        subtitle = "自定义模型与服务连接，配置后 AI 助手将使用此模型",
        bottomPadding = bottomPadding,
    ) {
        item {
            Text(
                text = "模型服务",
                style = KuaiMaoType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
            )
        }

        item {
            KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
                    Text(
                        text = "提供方",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = KuaiMaoColors.textMuted,
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

                    KuaiMaoFieldLabel(text = "API 地址")
                    KuaiMaoTextField(
                        value = settings.baseUrl,
                        onValueChange = { value -> update { it.copy(baseUrl = value) } },
                        placeholder = "https://api.openai.com/v1",
                        keyboardType = KeyboardType.Uri,
                    )

                    KuaiMaoFieldLabel(text = "API Key")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KuaiMaoTextField(
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
                                .background(KuaiMaoColors.canvasField)
                                .clickable { apiKeyVisible = !apiKeyVisible }
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                        ) {
                            Text(
                                text = if (apiKeyVisible) "隐藏" else "显示",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = KuaiMaoColors.textSec,
                            )
                        }
                    }

                    KuaiMaoFieldLabel(text = "模型名称")
                    KuaiMaoTextField(
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
                style = KuaiMaoType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
            )
        }

        item {
            KuaiMaoCard {
                KuaiMaoSwitchRow(
                    icon = Icons.TwoTone.Language,
                    title = "联网搜索",
                    description = "允许助手访问网络获取最新信息",
                    checked = settings.webSearch,
                    tone = KuaiMaoTones.blue,
                    onCheckedChange = { value -> update { it.copy(webSearch = value) } },
                )
                KuaiMaoRowDivider()
                KuaiMaoSwitchRow(
                    icon = Icons.TwoTone.Mic,
                    title = "语音输入",
                    description = "在对话中显示麦克风，长按说话",
                    checked = settings.voiceInput,
                    tone = KuaiMaoTones.violet,
                    onCheckedChange = { value -> update { it.copy(voiceInput = value) } },
                )
                KuaiMaoRowDivider()
                KuaiMaoSwitchRow(
                    icon = Icons.TwoTone.VerifiedUser,
                    title = "执行前确认",
                    description = "涉及重启、卸载等操作时先询问",
                    checked = settings.confirmBeforeRun,
                    tone = KuaiMaoTones.green,
                    onCheckedChange = { value -> update { it.copy(confirmBeforeRun = value) } },
                )
            }
        }

        item {
            Text(
                text = "生成参数",
                style = KuaiMaoType.sectionTitle,
                modifier = Modifier.padding(start = 2.dp, top = 20.dp, bottom = 10.dp),
            )
        }

        item {
            KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
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
                        colors = kuaimaoSliderColors(),
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
                        colors = kuaimaoSliderColors(),
                    )
                }

                KuaiMaoFieldLabel(text = "系统提示词")
                KuaiMaoTextField(
                    value = settings.systemPrompt,
                    onValueChange = { value -> update { it.copy(systemPrompt = value) } },
                    singleLine = false,
                    minLines = 3,
                )
            }
        }

        item {
            KuaiMaoPrimaryButton(
                text = if (testing) "测速中…" else "测试连接",
                enabled = !testing && settings.baseUrl.isNotBlank(),
                modifier = Modifier.padding(top = 18.dp),
                onClick = {
                    if (testing) return@KuaiMaoPrimaryButton
                    testing = true
                    report = null
                    testError = null
                    scope.launch {
                        // 真实测速：连接耗时、整轮延迟、服务端 token 统计都来自这次请求
                        val result = KuaiMaoAiClient.measure(settings)
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
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "实测结果",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = KuaiMaoColors.textMuted,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricCell(
                            label = "响应延迟",
                            value = formatMs(data.latencyMs),
                            tone = KuaiMaoColors.success,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCell(
                            label = "生成速度",
                            value = String.format("%.1f tok/s", data.tokensPerSecond),
                            tone = KuaiMaoColors.brand,
                            modifier = Modifier.weight(1f),
                        )
                        MetricCell(
                            label = "连接耗时",
                            value = formatMs(data.connectMs),
                            tone = KuaiMaoColors.textLabel,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "模型 ${data.model} · 输出 ${data.completionTokens} tokens" +
                            if (data.promptTokens > 0) " · 输入 ${data.promptTokens} tokens" else "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.textMuted,
                    )
                }
            }

            testError?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "连接失败",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = KuaiMaoColors.danger,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = KuaiMaoColors.textSec,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            KuaiMaoGhostButton(
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
                color = KuaiMaoColors.textMuted,
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
                                        colors = listOf(KuaiMaoColors.brand, KuaiMaoColors.brandDeep),
                                        start = Offset.Zero,
                                        end = Offset.Infinite,
                                    )
                                } else {
                                    SolidColor(KuaiMaoColors.canvasField)
                                },
                            )
                            .clickable { onSelect(provider) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = provider.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (selected) Color.White else KuaiMaoColors.textSec,
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
                color = KuaiMaoColors.textLabel,
            )
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = KuaiMaoColors.brand,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun kuaimaoSliderColors() = SliderDefaults.colors(
    thumbColor = KuaiMaoColors.brand,
    activeTrackColor = KuaiMaoColors.brand,
    inactiveTrackColor = KuaiMaoColors.canvasSubtle,
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
            .background(KuaiMaoColors.canvasField)
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
            color = KuaiMaoColors.textMuted,
            maxLines = 1,
        )
    }
}
