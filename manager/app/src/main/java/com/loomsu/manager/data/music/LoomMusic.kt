package com.loomsu.manager.data.music

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/** 一首歌。封面与音频都是网络直链，不做本地缓存。 */
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: String?,
    val streamUrl: String,
    val durationSec: Int,
) {
    val durationText: String
        get() = if (durationSec <= 0) "--:--" else "%d:%02d".format(durationSec / 60, durationSec % 60)
}

data class MusicState(
    val track: MusicTrack? = null,
    val queue: List<MusicTrack> = emptyList(),
    val index: Int = -1,
    val playing: Boolean = false,
    val loading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null,
) {
    val hasTrack: Boolean get() = track != null
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * 在线音乐检索。
 *
 * 走 Audius 的公开接口：不需要 API Key，返回直接可播的流地址。
 * 搜索时会过滤掉 `is_streamable = false` 的曲目——这些曲子的文件被节点屏蔽，
 * 播到一半会 403，不如直接不展示。
 */
object LoomMusicApi {
    private const val BASE = "https://api.audius.co"
    private const val APP_NAME = "LoomSU"
    private const val TIMEOUT_MS = 15_000

    fun streamUrl(id: String): String = "$BASE/v1/tracks/$id/stream?app_name=$APP_NAME"

    suspend fun search(query: String, limit: Int = 24): Result<List<MusicTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val text = get("$BASE/v1/tracks/search?query=$encoded&app_name=$APP_NAME&limit=$limit")
                val data = JSONObject(text).optJSONArray("data") ?: return@runCatching emptyList()
                buildList {
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        if (!item.optBoolean("is_streamable", true)) continue
                        val id = item.optString("id")
                        val title = item.optString("title").ifBlank { "未命名" }
                        if (id.isBlank()) continue
                        val user = item.optJSONObject("user")
                        val artwork = item.optJSONObject("artwork")
                        add(
                            MusicTrack(
                                id = id,
                                title = title,
                                artist = user?.optString("name").orEmpty().ifBlank { "未知艺人" },
                                artwork = artwork?.optString("150x150")?.takeIf { it.isNotBlank() },
                                streamUrl = streamUrl(id),
                                durationSec = item.optInt("duration", 0),
                            ),
                        )
                    }
                }
            }
        }

    /** 首页推荐：热门曲目，打开音乐页时先有内容，不用先搜索。 */
    suspend fun trending(limit: Int = 20): Result<List<MusicTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = get("$BASE/v1/tracks/trending?app_name=$APP_NAME&limit=$limit")
                val data = JSONObject(text).optJSONArray("data") ?: return@runCatching emptyList()
                buildList {
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        if (!item.optBoolean("is_streamable", true)) continue
                        val id = item.optString("id")
                        if (id.isBlank()) continue
                        val user = item.optJSONObject("user")
                        add(
                            MusicTrack(
                                id = id,
                                title = item.optString("title").ifBlank { "未命名" },
                                artist = user?.optString("name").orEmpty().ifBlank { "未知艺人" },
                                artwork = item.optJSONObject("artwork")?.optString("150x150"),
                                streamUrl = streamUrl(id),
                                durationSec = item.optInt("duration", 0),
                            ),
                        )
                    }
                }
            }
        }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) error("HTTP $code")
            return text
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * 全局唯一的播放器。
 *
 * 不挂在任何页面里：页面关掉了歌还得接着放，主页的迷你条也要能控制它。
 * 用系统 MediaPlayer，不引第三方库；只支持网络直链与本地路径。
 */
object LoomMusicPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var ticker: Job? = null

    fun playTrack(track: MusicTrack, queue: List<MusicTrack> = listOf(track)) {
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _state.update {
            it.copy(track = track, queue = queue, index = index, loading = true, error = null)
        }
        start(track)
    }

    fun playQueue(queue: List<MusicTrack>, startIndex: Int) {
        if (queue.isEmpty()) return
        val index = startIndex.coerceIn(0, queue.lastIndex)
        _state.update {
            it.copy(track = queue[index], queue = queue, index = index, loading = true, error = null)
        }
        start(queue[index])
    }

    fun toggle() {
        val mp = player ?: run {
            // 没有实例时按「继续上次那首」处理
            val track = _state.value.track ?: return
            start(track)
            return
        }
        runCatching {
            if (mp.isPlaying) {
                mp.pause()
                _state.update { it.copy(playing = false) }
            } else {
                mp.start()
                _state.update { it.copy(playing = true) }
            }
        }
    }

    fun next() = step(1)

    fun previous() = step(-1)

    private fun step(delta: Int) {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val target = (s.index + delta).mod(s.queue.size)
        playQueue(s.queue, target)
    }

    fun seekTo(ms: Long) {
        player?.let { runCatching { it.seekTo(ms.toInt()) } }
        _state.update { it.copy(positionMs = ms) }
    }

    /** 停止并清空：迷你条上的关闭按钮。 */
    fun stop() {
        ticker?.cancel()
        ticker = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        _state.value = MusicState()
    }

    private fun start(track: MusicTrack) {
        ticker?.cancel()
        runCatching { player?.release() }
        player = null
        val mp = MediaPlayer()
        player = mp
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        mp.setOnPreparedListener {
            it.start()
            _state.update { s ->
                s.copy(playing = true, loading = false, durationMs = it.duration.toLong().coerceAtLeast(0L))
            }
            startTicker()
        }
        mp.setOnCompletionListener { next() }
        mp.setOnErrorListener { _, what, extra ->
            _state.update {
                it.copy(playing = false, loading = false, error = "播放失败（$what/$extra），试试换一首")
            }
            true
        }
        runCatching {
            mp.setDataSource(track.streamUrl)
            mp.prepareAsync()
        }.onFailure { e ->
            _state.update {
                it.copy(playing = false, loading = false, error = e.message ?: "无法打开音频")
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val mp = player ?: break
                val position = runCatching { mp.currentPosition.toLong() }.getOrDefault(0L)
                val duration = runCatching { mp.duration.toLong() }.getOrDefault(0L)
                _state.update {
                    it.copy(
                        positionMs = position.coerceAtLeast(0L),
                        durationMs = if (duration > 0) duration else it.durationMs,
                    )
                }
                delay(500)
            }
        }
    }
}
