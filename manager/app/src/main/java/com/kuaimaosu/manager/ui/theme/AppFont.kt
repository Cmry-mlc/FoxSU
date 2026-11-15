package com.kuaimaosu.manager.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * 一套可下载的应用字体。
 *
 * 全部指向开源中文字体（思源黑体 / 霞鹜文楷 / 站酷系列 / 手写体），
 * 走 jsDelivr 的 Google Fonts 镜像，国内直连基本能跑满带宽。
 */
data class AppFont(
    val id: String,
    val name: String,
    val desc: String,
    val url: String,
    /** 用于 chunked 响应时估算进度（字节）。 */
    val approxBytes: Long,
)

object AppFonts {
    const val SYSTEM_ID = "system"
    const val CUSTOM_ID = "custom"

    private const val MIRROR = "https://cdn.jsdelivr.net/gh/google/fonts@main/ofl"

    val catalog: List<AppFont> = listOf(
        AppFont(
            id = "noto_sans_sc",
            name = "思源黑体",
            desc = "中性无衬线，正文最稳",
            url = "$MIRROR/notosanssc/NotoSansSC%5Bwght%5D.ttf",
            approxBytes = 10_500_000L,
        ),
        AppFont(
            id = "lxgw_wenkai",
            name = "霞鹜文楷",
            desc = "手写楷体，阅读柔和",
            url = "https://cdn.jsdelivr.net/gh/lxgw/LxgwWenKai@latest/fonts/TTF/LXGWWenKai-Regular.ttf",
            approxBytes = 19_000_000L,
        ),
        AppFont(
            id = "lxgw_wenkai_lite",
            name = "霞鹜文楷 Lite",
            desc = "文楷轻量版，体积小一半",
            url = "https://cdn.jsdelivr.net/gh/lxgw/LxgwWenKai-Lite@latest/fonts/TTF/LXGWWenKaiLite-Regular.ttf",
            approxBytes = 8_500_000L,
        ),
        AppFont(
            id = "zcool_kuaile",
            name = "站酷快乐体",
            desc = "圆润活泼，适合标题",
            url = "$MIRROR/zcoolkuaile/ZCOOLKuaiLe-Regular.ttf",
            approxBytes = 4_200_000L,
        ),
        AppFont(
            id = "zcool_qingke",
            name = "站酷庆科黄油体",
            desc = "粗壮厚实，冲击力强",
            url = "$MIRROR/zcoolqingkehuangyou/ZCOOLQingKeHuangYou-Regular.ttf",
            approxBytes = 5_600_000L,
        ),
        AppFont(
            id = "zcool_xiaowei",
            name = "站酷小薇体",
            desc = "清秀细体，安静克制",
            url = "$MIRROR/zcoolxiaowei/ZCOOLXiaoWei-Regular.ttf",
            approxBytes = 3_800_000L,
        ),
        AppFont(
            id = "mashanzheng",
            name = "马善政毛笔楷书",
            desc = "毛笔楷书，笔锋明显",
            url = "$MIRROR/mashanzheng/MaShanZheng-Regular.ttf",
            approxBytes = 5_100_000L,
        ),
        AppFont(
            id = "longcang",
            name = "龙藏体",
            desc = "行楷手写，气场沉稳",
            url = "$MIRROR/longcang/LongCang-Regular.ttf",
            approxBytes = 4_400_000L,
        ),
        AppFont(
            id = "zhimangxing",
            name = "志莽行书",
            desc = "行书连笔，适合短句",
            url = "$MIRROR/zhimangxing/ZhiMangXing-Regular.ttf",
            approxBytes = 6_800_000L,
        ),
        AppFont(
            id = "liujianmaocao",
            name = "刘建毛草",
            desc = "毛草狂放，点缀用",
            url = "$MIRROR/liujianmaocao/LiuJianMaoCao-Regular.ttf",
            approxBytes = 5_300_000L,
        ),
    )

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
    val context: Context = androidx.compose.ui.platform.LocalContext.current
    val id = config.appFontId
    return remember(id, config.appFontId) {
        when {
            id == AppFonts.SYSTEM_ID -> FontFamily.Default
            else -> {
                val file = AppFonts.fontFile(context, id)
                if (file.exists() && file.length() > 64 * 1024) {
                    runCatching { FontFamily(Font(file)) }.getOrDefault(FontFamily.Default)
                } else {
                    FontFamily.Default
                }
            }
        }
    }
}

/** 下载字体到私有目录，回调 0f..1f。 */
object FontDownloader {
    suspend fun download(
        context: Context,
        font: AppFont,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = AppFonts.fontFile(context, font.id)
            val tmp = File(target.parentFile, "${font.id}.part")
            if (tmp.exists()) tmp.delete()

            val connection = (URL(font.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android)")
            }
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) error("字体服务器返回 $code")

            val total = connection.contentLengthLong.takeIf { it > 0 } ?: font.approxBytes
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        read += count
                        onProgress((read.toFloat() / total).coerceIn(0f, 0.99f))
                    }
                }
            }
            connection.disconnect()

            if (tmp.length() < 64 * 1024) {
                tmp.delete()
                error("下载中断，文件不完整")
            }
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            onProgress(1f)
            target
        }
    }
}
