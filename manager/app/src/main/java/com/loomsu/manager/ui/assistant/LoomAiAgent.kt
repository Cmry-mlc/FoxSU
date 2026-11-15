package com.loomsu.manager.ui.assistant

import com.loomsu.manager.data.ai.AiSettings
import com.loomsu.manager.data.ai.AiToolCall
import com.loomsu.manager.data.ai.LoomAiClient
import com.loomsu.manager.ui.theme.BottomBarStyle
import com.loomsu.manager.ui.theme.CapsuleEffect
import org.json.JSONArray
import org.json.JSONObject

/**
 * 把模型接成「能动手」的助手。
 *
 * 模型不再只是回一段话：它可以选择调用 open_page / set_capsule_effect /
 * play_music 这些工具，由 [LoomAssistantRuntime] 真正执行，再把执行结果
 * 回填给模型，让它用一句人话总结。最多四轮，避免模型反复试错卡死。
 */
object LoomAiAgent {
    private const val MAX_ROUNDS = 4

    private val toolSpec: JSONArray by lazy {
        JSONArray().apply {
            put(
                function(
                    name = "open_page",
                    description = "打开 Loom SU 里的一个页面。用户说「打开 X」「去 X 看看」时用。",
                    properties = JSONObject().put(
                        "page_id",
                        JSONObject()
                            .put("type", "string")
                            .put("description", "页面 id")
                            .put(
                                "enum",
                                JSONArray(AppDestinations.all.map { it.id }),
                            ),
                    ),
                    required = listOf("page_id"),
                ),
            )
            put(
                function(
                    name = "set_capsule_effect",
                    description = "设置底栏 / 顶栏的材质质感。",
                    properties = JSONObject().put(
                        "effect",
                        JSONObject()
                            .put("type", "string")
                            .put("enum", JSONArray(listOf("liquid_glass", "frosted", "translucent"))),
                    ),
                    required = listOf("effect"),
                ),
            )
            put(
                function(
                    name = "set_bottom_bar_style",
                    description = "切换底栏样式：胶囊浮岛 / 传统贴底 / 侧边栏。",
                    properties = JSONObject().put(
                        "style",
                        JSONObject()
                            .put("type", "string")
                            .put("enum", JSONArray(listOf("capsule", "traditional", "rail"))),
                    ),
                    required = listOf("style"),
                ),
            )
            put(
                function(
                    name = "play_music",
                    description = "在线搜索并播放音乐。用户说「放首歌」「播放周杰伦」时用；" +
                        "没给歌名就传空字符串，打开音乐页并播放热门。",
                    properties = JSONObject().put(
                        "query",
                        JSONObject()
                            .put("type", "string")
                            .put("description", "歌名 / 歌手，没有就传空字符串"),
                    ),
                    required = listOf("query"),
                ),
            )
            put(
                function(
                    name = "music_control",
                    description = "控制正在播放的音乐。",
                    properties = JSONObject().put(
                        "command",
                        JSONObject()
                            .put("type", "string")
                            .put("enum", JSONArray(listOf("pause", "resume", "next", "previous", "stop"))),
                    ),
                    required = listOf("command"),
                ),
            )
            put(
                function(
                    name = "set_volume",
                    description = "设置媒体音量百分比（0-100）。",
                    properties = JSONObject().put(
                        "percent",
                        JSONObject().put("type", "integer"),
                    ),
                    required = listOf("percent"),
                ),
            )
            put(
                function(
                    name = "set_blur",
                    description = "开关全局背景模糊。",
                    properties = JSONObject().put(
                        "enabled",
                        JSONObject().put("type", "boolean"),
                    ),
                    required = listOf("enabled"),
                ),
            )
            put(
                function(
                    name = "set_background_dim",
                    description = "设置自定义壁纸的暗度百分比（0-100）。",
                    properties = JSONObject().put(
                        "percent",
                        JSONObject().put("type", "integer"),
                    ),
                    required = listOf("percent"),
                ),
            )
        }
    }

    private fun function(
        name: String,
        description: String,
        properties: JSONObject,
        required: List<String>,
    ): JSONObject = JSONObject().apply {
        put("type", "function")
        put(
            "function",
            JSONObject()
                .put("name", name)
                .put("description", description)
                .put(
                    "parameters",
                    JSONObject()
                        .put("type", "object")
                        .put("properties", properties)
                        .put("required", JSONArray(required)),
                ),
        )
    }

    suspend fun run(
        settings: AiSettings,
        history: List<Pair<Boolean, String>>,
        onStep: (String) -> Unit = {},
    ): Result<String> {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt(settings)))
            history.forEach { (fromUser, text) ->
                put(
                    JSONObject()
                        .put("role", if (fromUser) "user" else "assistant")
                        .put("content", text),
                )
            }
        }

        var lastText = ""
        repeat(MAX_ROUNDS) {
            val turn = LoomAiClient.chatTurn(settings, messages, toolSpec)
                .getOrElse { return Result.failure(it) }

            lastText = turn.content.ifBlank { lastText }
            if (turn.toolCalls.isEmpty()) {
                return Result.success(turn.content.ifBlank { "已处理完成。" })
            }

            messages.put(
                JSONObject()
                    .put("role", "assistant")
                    .put("content", turn.content)
                    .put(
                        "tool_calls",
                        JSONArray().apply {
                            turn.toolCalls.forEach { call ->
                                put(
                                    JSONObject()
                                        .put("id", call.id)
                                        .put("type", "function")
                                        .put(
                                            "function",
                                            JSONObject()
                                                .put("name", call.name)
                                                .put("arguments", call.arguments),
                                        ),
                                )
                            }
                        },
                    ),
            )

            turn.toolCalls.forEach { call ->
                val result = executeTool(call)
                onStep(result)
                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.id)
                        .put("content", result),
                )
            }
        }

        return Result.success(lastText.ifBlank { "已经按你说的做了。" })
    }

    private fun executeTool(call: AiToolCall): String {
        val args = runCatching { JSONObject(call.arguments.ifBlank { "{}" }) }.getOrDefault(JSONObject())
        val action: AssistantAction? = when (call.name) {
            "open_page" -> AppDestinations.byId(args.optString("page_id"))
                ?.let { AssistantAction.OpenPage(it.id) }

            "set_capsule_effect" -> CapsuleEffect.entries
                .firstOrNull { it.id == args.optString("effect") }
                ?.let { AssistantAction.SetCapsuleEffect(it) }

            "set_bottom_bar_style" -> when (args.optString("style")) {
                "capsule" -> AssistantAction.SetBottomBarStyle(BottomBarStyle.CAPSULE)
                "traditional" -> AssistantAction.SetBottomBarStyle(BottomBarStyle.TRADITIONAL)
                "rail" -> AssistantAction.SetBottomBarStyle(BottomBarStyle.RAIL)
                else -> null
            }

            "play_music" -> AssistantAction.PlayMusic(args.optString("query").takeIf { it.isNotBlank() })
            "music_control" -> AssistantAction.MusicControl(args.optString("command"))
            "set_volume" -> AssistantAction.SetVolume(args.optInt("percent", 50))
            "set_blur" -> AssistantAction.SetBlur(args.optBoolean("enabled", true))
            "set_background_dim" -> AssistantAction.SetBackgroundDim(args.optInt("percent", 0))
            else -> null
        }
        return action?.let { LoomAssistantRuntime.execute(it) } ?: "无法执行 ${call.name}"
    }

    private fun systemPrompt(settings: AiSettings): String = buildString {
        append(settings.systemPrompt)
        append("\n\n你可以直接操作 App：打开页面、切换底栏样式与质感、开关模糊、调暗度、搜歌播放、控制播放与音量。")
        append("用户给出明确指令时，优先调用工具执行，再简短说明结果；不要只回复「请手动打开」。")
        append("\n可打开的页面：")
        append(AppDestinations.all.joinToString("；") { "${it.id}=${it.label}" })
    }
}
