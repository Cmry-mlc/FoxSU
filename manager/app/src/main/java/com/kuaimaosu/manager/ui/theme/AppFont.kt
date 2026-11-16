package com.kuaimaosu.manager.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.kuaimaosu.manager.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * 一套可下载的应用字体。
 *
 * 全部指向开源中文字体（思源黑体 / 霞鹜文楷 / 站酷系列 / 手写体）。
 * 只存「仓库 + 引用 + 路径」，具体下载地址由 [AppFonts.sourcesOf] 现场拼出来：
 * jsDelivr 的三个节点 + 两个 GitHub 反代 + raw 直连，逐个试，谁通就用谁。
 */
data class AppFont(
    val id: String,
    val name: String,
    val desc: String,
    /** owner/repo */
    val repo: String,
    /** jsDelivr 用的引用：分支或 tag。 */
    val ref: String,
    /** raw.githubusercontent.com 用的引用（tag 不能当分支用，这里显式写分支）。 */
    val rawRef: String,
    val path: String,
    /** 用于 chunked 响应时估算进度（字节）。 */
    val approxBytes: Long,
) {
    val sources: List<String> get() = AppFonts.sourcesOf(this)
}

object AppFonts {
    const val SYSTEM_ID = "system"
    const val CUSTOM_ID = "custom"

    val catalog: List<AppFont> = listOf(
        AppFont(
            id = "noto_sans_sc",
            name = "思源黑体",
            desc = "中性无衬线，正文最稳",
            repo = "google/fonts",
            ref = "main",
            rawRef = "main",
            path = "ofl/notosanssc/NotoSansSC%5Bwght%5D.ttf",
            approxBytes = 17_772_300L,
        ),
        AppFont(
            id = "lxgw_wenkai",
            name = "霞鹜文楷",
            desc = "手写楷体，阅读柔和",
            repo = "lxgw/LxgwWenKai",
            ref = "latest",
            rawRef = "master",
            path = "fonts/TTF/LXGWWenKai-Regular.ttf",
            approxBytes = 15_623_888L,
        ),
        AppFont(
            id = "lxgw_wenkai_lite",
            name = "霞鹜文楷 Lite",
            desc = "文楷轻量版，体积小一半",
            repo = "lxgw/LxgwWenKai-Lite",
            ref = "latest",
            rawRef = "main",
            path = "fonts/TTF/LXGWWenKaiLite-Regular.ttf",
            approxBytes = 9_752_448L,
        ),
        AppFont(
            id = "zcool_kuaile",
            name = "站酷快乐体",
            desc = "圆润活泼，适合标题",
            repo = "google/fonts",
            ref = "main",
            rawRef = "main",
            path = "ofl/zcoolkuaile/ZCOOLKuaiLe-Regular.ttf",
            approxBytes = 1_514_968L,
        ),
        AppFont(
            id = "zcool_qingke",
            name = "站酷庆科黄油体",
            desc = "粗壮厚实，冲击力强",
            repo = "google/fonts",
            ref = "main",
            rawRef = "main",
            path = "ofl/zcoolqingkehuangyou/ZCOOLQingKeHuangYou-Regular.ttf",
            approxBytes = 8_328_684L,
        ),
        AppFont(
            id = "zcool_xiaowei",
            name = "站酷小薇体",
            desc = "清秀细体，安静克制",
            repo = "google/fonts",
            ref = "main",
            rawRef = "main",
            path = "ofl/zcoolxiaowei/ZCOOLXiaoWei-Regular.ttf",
            approxBytes = 6_313_808L,
        ),
        AppFont(
            id = "mashanzheng",
            name = "马善政毛笔楷书",
            desc = "毛笔楷书，笔锋明显",
            repo = "google/fonts",
            ref = "main",
            rawRef = "main",
            path = "ofl/mashanzheng/MaShanZheng-Regular.ttf",
            approxBytes = 5_857_936L,
        ),
        AppFont(
            id = "longcang",
            name = "龙藏体",
            desc = "行楷手写，气场沉稳",
            repo = "google/fonts",
            ref = "main",
            rawRef = "main",
            path = "ofl/longcang/LongCang-Regular.ttf",
            approxBytes = 5_162_508L,
        ),
        AppFont(
            id = "zhimangxing",
            name = "志莽行书",
            desc = "行书连笔，适合短句",
            repo = "google/fonts",
            ref = "main",
            rawRef = "main",
            path = "ofl/zhimangxing/ZhiMangXing-Regular.ttf",
            approxBytes = 4_063_532L,
        ),
        AppFont(
            id = "liujianmaocao",
            name = "刘建毛草",
            desc = "毛草狂放，点缀用",
            repo = "google/fonts",
            ref = "main",
            rawRef = "main",
            path = "ofl/liujianmaocao/LiuJianMaoCao-Regular.ttf",
            approxBytes = 4_951_804L,
        ),
    )

    /**
     * 一个字体要试的所有地址，按「国内最容易连上」排序。
     *
     * 之前只用 cdn.jsdelivr.net 一个域名：运营商把 DNS 一污染，十个字体全部下不动，
     * 用户看到的就是「一个都下载不下来」。现在换域、换 CDN、换反代，五个源里
     * 只要有一个通就能落盘。
     */
    fun sourcesOf(font: AppFont): List<String> {
        val jsdelivrPath = "gh/${font.repo}@${font.ref}/${font.path}"
        val rawPath = "https://raw.githubusercontent.com/${font.repo}/${font.rawRef}/${font.path}"
        return listOf(
            "https://fastly.jsdelivr.net/$jsdelivrPath",
            "https://cdn.jsdelivr.net/$jsdelivrPath",
            "https://gcore.jsdelivr.net/$jsdelivrPath",
            "https://ghfast.top/$rawPath",
            "https://gh-proxy.com/$rawPath",
            rawPath,
        )
    }

    fun find(id: String): AppFont? = catalog.firstOrNull { it.id == id }

    fun fontDir(context: Context): File = File(context.filesDir, "app_fonts").apply { mkdirs() }

    fun fontFile(context: Context, id: String): File = File(fontDir(context), "$id.ttf")

    fun isInstalled(context: Context, id: String): Boolean =
        id != SYSTEM_ID && fontFile(context, id).let { it.exists() && it.length() > 64 * 1024 }
}

/**
 * 全局字体：在 [KuaiMaoTheme] 里提供，[KuaiMaoType] 与 MaterialTheme.typography 都读它。
 */
val LocalKuaiMaoFontFamily = androidx.compose.runtime.compositionLocalOf<FontFamily> { FontFamily.Default }

/** 当前生效的字体族。读 [ThemeConfig]，字体文件缺失时自动退回系统字体。 */
@Composable
fun rememberAppFontFamily(): FontFamily {
    val config: ThemeConfig = koinInject()
    val context: Context = LocalContext.current
    val id = config.appFontId
    return remember(id, config.appFontId) {
        when {
            id == AppFonts.SYSTEM_ID -> FontFamily.Default
            else -> loadLocalFontFamily(context, id) ?: FontFamily.Default
        }
    }
}

/** 读私有目录里的字体文件，供选择器逐项预览用；没有文件就返回 null。 */
fun loadLocalFontFamily(context: Context, id: String): FontFamily? {
    if (id == AppFonts.SYSTEM_ID) return null
    val file = AppFonts.fontFile(context, id)
    if (!file.exists() || file.length() <= 64 * 1024) return null
    return runCatching { FontFamily(Font(file)) }.getOrNull()
}

/** 下载字体到私有目录，回调 0f..1f。 */
object FontDownloader {
    private const val MIN_FONT_BYTES = 64 * 1024L

    suspend fun download(
        context: Context,
        font: AppFont,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (source in font.sources) {
            val result = runCatching { downloadFrom(context, font, source, onProgress) }
            result.getOrNull()?.let { return@withContext Result.success(it) }
            val failure = result.exceptionOrNull()
            // 协程被取消（用户关掉面板）时不要再往下试源，直接把取消抛出去。
            if (failure is CancellationException) throw failure
            lastError = failure
        }
        val reason = lastError?.message ?: "网络不可用"
        Result.failure(
            IllegalStateException(
                "所有下载节点都失败了（${font.sources.size} 个）：$reason",
                lastError,
            ),
        )
    }

    private fun downloadFrom(
        context: Context,
        font: AppFont,
        source: String,
        onProgress: (Float) -> Unit,
    ): File {
        val target = AppFonts.fontFile(context, font.id)
        val tmp = File(target.parentFile, "${font.id}.part")
        if (tmp.exists()) tmp.delete()

        val connection = (URL(source).openConnection() as HttpURLConnection).apply {
            // 连接只等 8 秒：一个源连不上就赶紧换下一个，别让用户盯着 0% 发呆。
            connectTimeout = 8_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "KuaiMaoSU/${BuildConfig.VERSION_NAME} (Android)")
            setRequestProperty("Accept", "font/ttf,font/otf,application/octet-stream,*/*")
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            // 运营商劫持 / 镜像报错页会以 200 返回一段 HTML，写进去就是坏文件。
            val contentType = connection.contentType.orEmpty()
            if (contentType.contains("text/html", ignoreCase = true)) {
                error("返回的是网页，不是字体")
            }

            val total = connection.contentLengthLong.takeIf { it > 0 } ?: font.approxBytes
            var lastReported = -1
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        read += count
                        val percent = ((read * 100) / total).toInt().coerceIn(0, 99)
                        if (percent != lastReported) {
                            lastReported = percent
                            onProgress(percent / 100f)
                        }
                    }
                }
            }

            if (tmp.length() < MIN_FONT_BYTES) {
                tmp.delete()
                error("下载中断，只拿到 ${tmp.length()} 字节")
            }
            if (!looksLikeFont(tmp)) {
                tmp.delete()
                error("文件头不是字体")
            }
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            onProgress(1f)
            return target
        } finally {
            connection.disconnect()
        }
    }

    /** ttf / otf / ttc 的文件头校验，挡住 HTML 错误页和空壳文件。 */
    private fun looksLikeFont(file: File): Boolean {
        if (file.length() < MIN_FONT_BYTES) return false
        return runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                if (input.read(header) != 4) return@use false
                val tag = String(header, Charsets.ISO_8859_1)
                tag == "OTTO" || tag == "true" || tag == "ttcf" ||
                    (header[0] == 0.toByte() && header[1] == 1.toByte() &&
                        header[2] == 0.toByte() && header[3] == 0.toByte())
            }
        }.getOrDefault(false)
    }
}
