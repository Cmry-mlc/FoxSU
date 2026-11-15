package com.kuaimaosu.manager.data.ai

import android.content.Context
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * KuaiMaoSU 的 AI 服务配置。
 *
 * 仅保存在本机 SharedPreferences（应用私有目录），不上传、不参与云同步。
 * 配置为空时 AI 助手自动回退到内置的本地规则应答。
 */
data class AiSettings(
    val providerId: String = AiProviders.DEFAULT_ID,
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val webSearch: Boolean = true,
    val voiceInput: Boolean = true,
    val confirmBeforeRun: Boolean = true,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val systemPrompt: String =
        "你是 KuaiMaoSU 内置助手，负责解答内核、Root 与模块相关问题，回答保持简洁专业。" +
            "你能直接操作 App：切换底栏样式与质感、开关模糊、打开任意页面、搜歌播放与控制播放。",
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}

data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
)

object AiProviders {
    const val DEFAULT_ID = "openai"

    val all: List<AiProvider> = listOf(
        AiProvider("openai", "OpenAI 兼容", "https://api.openai.com/v1", "gpt-4o-mini"),
        AiProvider("anthropic", "Anthropic", "https://api.anthropic.com/v1", "claude-sonnet-4"),
        AiProvider("doubao", "豆包 Doubao", "https://ark.cn-beijing.volces.com/api/v3", "doubao-seed-1-6"),
        AiProvider("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
        AiProvider("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
        AiProvider("kimi", "Kimi", "https://api.moonshot.cn/v1", "kimi-k2"),
        AiProvider("glm", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "glm-4-plus"),
        AiProvider("custom", "自定义", "", ""),
    )

    fun find(id: String): AiProvider? = all.firstOrNull { it.id == id }
}

object AiSettingsStore {
    private const val PREF = "kuaimao_ai_settings"

    fun load(context: Context): AiSettings {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val defaults = AiSettings()
        return AiSettings(
            providerId = p.getString("providerId", defaults.providerId) ?: defaults.providerId,
            baseUrl = p.getString("baseUrl", defaults.baseUrl) ?: defaults.baseUrl,
            apiKey = p.getString("apiKey", defaults.apiKey) ?: defaults.apiKey,
            model = p.getString("model", defaults.model) ?: defaults.model,
            webSearch = p.getBoolean("webSearch", defaults.webSearch),
            voiceInput = p.getBoolean("voiceInput", defaults.voiceInput),
            confirmBeforeRun = p.getBoolean("confirmBeforeRun", defaults.confirmBeforeRun),
            temperature = p.getFloat("temperature", defaults.temperature),
            maxTokens = p.getInt("maxTokens", defaults.maxTokens),
            systemPrompt = p.getString("systemPrompt", defaults.systemPrompt) ?: defaults.systemPrompt,
        )
    }

    fun save(context: Context, settings: AiSettings) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().apply {
            putString("providerId", settings.providerId)
            putString("baseUrl", settings.baseUrl)
            putString("apiKey", settings.apiKey)
            putString("model", settings.model)
            putBoolean("webSearch", settings.webSearch)
            putBoolean("voiceInput", settings.voiceInput)
            putBoolean("confirmBeforeRun", settings.confirmBeforeRun)
            putFloat("temperature", settings.temperature)
            putInt("maxTokens", settings.maxTokens)
            putString("systemPrompt", settings.systemPrompt)
        }.apply()
    }

    fun clearKey(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove("apiKey").apply()
    }
}

/**
 * 一次真实连接测速的结果。
 *
 * [connectMs] 是建立 TCP/TLS 连接的耗时，[latencyMs] 是整轮请求耗时，
 * [tokensPerSecond] 用服务端返回的 completion_tokens 除以实际耗时得到，
 * 都是实测值，不做任何估算填充。
 */
data class AiTestReport(
    val connectMs: Long,
    val latencyMs: Long,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val reply: String,
) {
    val tokensPerSecond: Double
        get() = if (latencyMs <= 0) 0.0 else completionTokens * 1000.0 / latencyMs
}

/**
 * 模型要求执行的一次工具调用。
 *
 * [arguments] 是模型原样返回的 JSON 字符串，由执行端自己解析，
 * 这样新增工具时不用改协议层。
 */
data class AiToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

/** 一轮模型的返回：要么是一段话，要么是若干个工具调用（也可能两者都有）。 */
data class AiTurn(
    val content: String,
    val toolCalls: List<AiToolCall> = emptyList(),
)

/**
 * 最小化的 OpenAI 兼容客户端。
 *
 * 只依赖 HttpURLConnection，避免为一次对话引入额外网络库。
 * 任何异常都返回失败，由调用方回退到本地规则应答。
 */
object KuaiMaoAiClient {
    private const val TIMEOUT_MS = 20_000
    suspend fun chat(settings: AiSettings, userMessage: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val messages = JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", settings.systemPrompt))
                    put(JSONObject().put("role", "user").put("content", userMessage))
                }
                val body = JSONObject()
                    .put("model", settings.model)
                    .put("messages", messages)
                    .put("temperature", settings.temperature.toDouble())
                    .put("max_tokens", settings.maxTokens)
                    .toString()

                val connection = (URL("${settings.baseUrl.trimEnd('/')}/chat/completions")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                }

                try {
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                    if (code !in 200..299) {
                        error("HTTP $code ${text.take(160)}")
                    }
                    val content = JSONObject(text)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        .orEmpty()
                    if (content.isBlank()) error("空响应") else content.trim()
                } finally {
                    connection.disconnect()
                }
            }
        }

    suspend fun testConnection(settings: AiSettings): Result<String> =
        chat(settings.copy(maxTokens = 32), "回复两个字：正常")

    /**
     * 真实测速：记录连接耗时与整轮耗时，并从响应里读 usage 统计。
     * 服务端没返回 usage 时按返回文本粗估（4 字符 ≈ 1 token），并在 UI 上标注。
     */
    suspend fun measure(settings: AiSettings): Result<AiTestReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                val messages = JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", "你是测速探针，只回复一句简短确认。"))
                    put(JSONObject().put("role", "user").put("content", "ping"))
                }
                val body = JSONObject()
                    .put("model", settings.model)
                    .put("messages", messages)
                    .put("temperature", 0.0)
                    .put("max_tokens", 32)
                    .toString()

                val startedAt = System.nanoTime()
                val connection = (URL("${settings.baseUrl.trimEnd('/')}/chat/completions")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                }

                try {
                    connection.connect()
                    val connectMs = (System.nanoTime() - startedAt) / 1_000_000
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                    val latencyMs = (System.nanoTime() - startedAt) / 1_000_000
                    if (code !in 200..299) {
                        error("HTTP $code ${text.take(160)}")
                    }
                    val root = JSONObject(text)
                    val choice = root.optJSONArray("choices")?.optJSONObject(0)
                    val content = choice?.optJSONObject("message")?.optString("content").orEmpty()
                    if (content.isBlank()) error("空响应")

                    val usage = root.optJSONObject("usage")
                    val promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0
                    val completionTokens = usage?.optInt("completion_tokens", 0)
                        ?.takeIf { it > 0 }
                        ?: (content.length / 4).coerceAtLeast(1)

                    AiTestReport(
                        connectMs = connectMs,
                        latencyMs = latencyMs,
                        model = root.optString("model").ifBlank { settings.model },
                        promptTokens = promptTokens,
                        completionTokens = completionTokens,
                        reply = content.trim().take(40),
                    )
                } finally {
                    connection.disconnect()
                }
            }
        }
    /**
     * 带工具调用的一轮对话。
     *
     * 和 [chat] 的区别只有两点：请求里带上 `tools`，响应里除了文本还解析
     * `tool_calls`。执行工具、把结果回填、再问一轮的流程放在
     * `KuaiMaoAiAgent` 里，这一层只负责协议。
     */
    suspend fun chatTurn(
        settings: AiSettings,
        messages: JSONArray,
        tools: JSONArray? = null,
    ): Result<AiTurn> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("model", settings.model)
                .put("messages", messages)
                .put("temperature", settings.temperature.toDouble())
                .put("max_tokens", settings.maxTokens)

            if (tools != null && tools.length() > 0) {
                payload.put("tools", tools)
                payload.put("tool_choice", "auto")
            }

            val connection = (URL("${settings.baseUrl.trimEnd('/')}/chat/completions")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
            }

            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                if (code !in 200..299) error("HTTP $code ${text.take(160)}")

                val message = JSONObject(text)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")

                val content = message?.optString("content").orEmpty().trim()
                val calls = buildList {
                    val array = message?.optJSONArray("tool_calls")
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val call = array.optJSONObject(i) ?: continue
                            val function = call.optJSONObject("function") ?: continue
                            add(
                                AiToolCall(
                                    id = call.optString("id"),
                                    name = function.optString("name"),
                                    arguments = function.optString("arguments"),
                                ),
                            )
                        }
                    }
                }
                if (content.isBlank() && calls.isEmpty()) error("空响应")
                AiTurn(content = content, toolCalls = calls)
            } finally {
                connection.disconnect()
            }
        }
    }
}
