package com.loomsu.manager.ui.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * 标题小字的本地诗集。
 *
 * [random] 是纯本地取句，不联网、不花钱，作为「随机一句」与未配置 AI 时的兜底，
 * 所以按钮永远有反馈。全部是中文旧体诗词，不带任何英文前缀。
 */
object LoomPoetry {

    /** 诗集：整句直接取用，长度控制在首页小字一行放得下的范围。 */
    private val curated = listOf(
        "山月不知心里事，水风空落眼前花。",
        "此心安处是吾乡。",
        "人生如逆旅，我亦是行人。",
        "一蓑烟雨任平生。",
        "海上生明月，天涯共此时。",
        "人间有味是清欢。",
        "落花人独立，微雨燕双飞。",
        "当时只道是寻常。",
        "星垂平野阔，月涌大江流。",
        "夜阑卧听风吹雨，铁马冰河入梦来。",
        "云想衣裳花想容，春风拂槛露华浓。",
        "无可奈何花落去，似曾相识燕归来。",
        "众里寻他千百度，蓦然回首，那人却在，灯火阑珊处。",
        "明月松间照，清泉石上流。",
        "行到水穷处，坐看云起时。",
        "春风得意马蹄疾，一日看尽长安花。",
        "青山依旧在，几度夕阳红。",
        "小舟从此逝，江海寄余生。",
        "醉后不知天在水，满船清梦压星河。",
        "疏影横斜水清浅，暗香浮动月黄昏。",
        "晚来天欲雪，能饮一杯无。",
        "但愿人长久，千里共婵娟。",
        "世事一场大梦，人生几度秋凉。",
        "溪云初起日沉阁，山雨欲来风满楼。",
    )

    /** 半句拼句用的短意象，避免每次都抽到同一整句。 */
    private val scenes = listOf(
        "月色落进旧窗",
        "风过竹林",
        "一灯如豆",
        "远山含黛",
        "夜雨敲檐",
        "残雪映阶",
        "茶烟初起",
        "孤舟横渡",
    )

    private val closures = listOf(
        "人间忽晚",
        "山河已秋",
        "此意难平",
        "各自珍重",
        "且听风吟",
        "不问归期",
        "灯火可亲",
        "岁月无声",
    )

    /** 诗集里的整句，供首页小字轮播按顺序取用。 */
    fun curated(): List<String> = curated

    /** 本地取一句，永远可用。 */
    fun random(): String {
        if (Random.nextInt(100) < 60) {
            return curated[Random.nextInt(curated.size)]
        }
        val scene = scenes[Random.nextInt(scenes.size)]
        val closure = closures[Random.nextInt(closures.size)]
        return "$scene，$closure。"
    }

    /**
     * 调用 OpenAI 兼容接口生成一句。
     *
     * [endpoint] 需要是完整的 chat/completions 地址，例如
     * `https://api.openai.com/v1/chat/completions`。
     */
    suspend fun generate(
        endpoint: String,
        apiKey: String,
        model: String,
        prompt: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(endpoint.isNotBlank()) { "接口地址为空" }
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
            }
            val payload = JSONObject().apply {
                put("model", model.ifBlank { "gpt-4o-mini" })
                put("temperature", 1.0)
                put("max_tokens", 48)
                put(
                    "messages",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("role", "system")
                                put(
                                    "content",
                                    "你是一位古典诗人。只输出一行中文旧体诗句，18 字以内，" +
                                        "不要引号，不要解释，不要英文，不要任何署名前缀。",
                                )
                            },
                        )
                        put(
                            JSONObject().apply {
                                put("role", "user")
                                put(
                                    "content",
                                    prompt.ifBlank { "写一句关于夜色、月光与旧事的短诗" },
                                )
                            },
                        )
                    },
                )
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }

            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            connection.disconnect()

            if (code !in 200..299) {
                error("接口返回 $code")
            }

            val content = JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .trim('"', '\'', '“', '”')
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")

            require(content.isNotBlank()) { "接口返回了空内容" }
            content.take(60)
        }
    }
}
