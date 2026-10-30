package me.weishu.kernelsu.ui.util

import android.util.Log
import com.topjohnwu.superuser.Shell
import me.weishu.kernelsu.ksuApp
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 系统级去广告（systemless hosts）。
 *
 * 原理：下载开源广告域名 hosts 列表 → 合并去重、套白名单 → 以 KernelSU 模块
 * （/data/adb/modules/foxsu_adblock/system/etc/hosts）的形式在开机时由 metamodule
 * bind-mount 覆盖 /system/etc/hosts。广告域名全部解析到 0.0.0.0，
 * 无 VPN、无后台进程、零内存占用。
 *
 * 规则源均为持续维护的开源仓库（2026-10 仍在更新）：
 *  - StevenBlack/hosts（聚合 AdAway/yoyo/URLHaus 等 14 个源）
 *  - AdAway 官方列表
 *  - neoHosts（国内广告 / 运营商劫持）
 * 支持自定义源 URL 与 GitHub 加速镜像。
 */
object AdBlock {

    private const val TAG = "AdBlock"
    private const val MODULE_DIR = "/data/adb/modules/foxsu_adblock"
    private const val WORK_DIR = "/data/adb/foxsu/adblock"

    data class Source(val id: String, val name: String, val desc: String, val url: String)

    /**
     * 规则源全部为国内来源（国外列表误杀国内 App 的比率高，且体积大）：
     *  - anti-AD：国内最权威的反广告域名库，每天更新，10 万+ 域名，覆盖国内 App / 运营商广告
     *  - AdGuard Chinese：AdGuard 官方中文过滤规则中的纯域名部分（国内站点广告网络）
     * 格式兼容 hosts / dnsmasq(address=/x/) / 纯域名 三种。
     */
    val SOURCES = listOf(
        Source(
            "antiad", "anti-AD 国内广告库", "10 万+ 国内 App / 运营商广告域名，每日更新（推荐）",
            "https://raw.githubusercontent.com/privacy-protection-tools/anti-AD/master/adblock-for-dnsmasq.conf",
        ),
        Source(
            "antiad_dom", "anti-AD 纯域名精简版", "同一库的纯域名列表，体积更小、解析更快",
            "https://raw.githubusercontent.com/privacy-protection-tools/anti-AD/master/anti-ad-domains.txt",
        ),
        Source(
            "adguard_cn", "AdGuard 中文过滤", "AdGuard 官方中文规则中的国内广告网络域名",
            "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/ChineseFilter/sections/adservers.txt",
        ),
    )

    data class Status(
        val installed: Boolean,   // 模块已安装
        val enabled: Boolean,     // 模块未被 disable
        val sourceId: String,
        val entries: Int,         // 当前 hosts 条目数
        val updatedAt: String,    // 上次更新时间（epoch ms，0 = 未更新过）
        val whitelist: List<String>,
    )

    private fun sh(): Shell = getRootShell()

    private fun cmd(shell: Shell, vararg commands: String): Pair<Int, List<String>> {
        val out = ArrayList<String>()
        val err = ArrayList<String>()
        val r = shell.newJob().add(*commands).to(out, err).exec()
        return r.code to (out + err).filter { it.isNotBlank() }
    }

    private fun escape(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    // ── 配置读写（存 /data/adb/foxsu/adblock/）────────────────────────────

    private fun cfg(shell: Shell, name: String): String {
        val (_, out) = cmd(shell, "[ -f $WORK_DIR/$name ] && cat $WORK_DIR/$name || true")
        return out.joinToString("\n").trim()
    }

    private fun setCfg(shell: Shell, name: String, value: String) {
        cmd(shell, "mkdir -p $WORK_DIR", "printf '%s' ${escape(value)} > $WORK_DIR/$name")
    }

    fun status(): Status {
        val shell = sh()
        val installed = cmd(shell, "[ -f $MODULE_DIR/module.prop ] && echo __ok__ || true").second.any { it.contains("__ok__") }
        val enabled = cmd(shell, "[ ! -f $MODULE_DIR/disable ] && echo __ok__ || true").second.any { it.contains("__ok__") }
        val entries = cfg(shell, "entries").toIntOrNull() ?: 0
        val updatedAt = cfg(shell, "updated_at")
        val wl = cfg(shell, "whitelist").split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        return Status(installed, enabled, cfg(shell, "source").ifBlank { "antiad" }, entries, updatedAt, wl)
    }

    // ── 下载与合并 ────────────────────────────────────────────────────────

    /** 应用侧直接 HTTP 下载（跟随跳转）；失败返回 null */
    fun download(url: String, useMirror: Boolean): ByteArray? {
        return try {
            val real = if (useMirror && url.contains("raw.githubusercontent.com"))
                "https://gh-proxy.com/$url" else url
            val conn = URL(real).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "download $real → ${conn.responseCode}")
                return null
            }
            conn.inputStream.use { it.readBytes() }.also { conn.disconnect() }
        } catch (e: Throwable) {
            Log.e(TAG, "download", e)
            null
        }
    }

    /**
     * 合并多个 hosts 文本：
     * 去注释/空行/内联注释、统一 0.0.0.0、按域名去重、套白名单、剔除本机保留域。
     */
    fun merge(texts: List<String>, whitelist: Set<String>): Pair<String, Int> {
        val keep = Regex("^(localhost|localhost\\.localdomain|local)$")
        val seen = HashSet<String>()
        val out = StringBuilder(
            "# FoxSu AdBlock · systemless hosts\n# 由 FoxSu Manager 生成，请勿直接编辑\n\n")
        var count = 0
        for (raw in texts) {
            for (line0 in raw.lineSequence()) {
                var line = line0.trim()
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue
                // dnsmasq 格式：address=/example.com/
                if (line.startsWith("address=/")) {
                    line = line.removePrefix("address=/").trimEnd('/').trim()
                } else {
                    line = line.substringBefore('#').trim()
                }
                if (line.isEmpty()) continue
                // AdGuard 规则的例外语法 @@||domain^ 表示放行，直接跳过
                if (line.startsWith("@@")) continue
                val domain = line.split(Regex("[\\s^|/]+")).filter { it.isNotEmpty() }
                    .lastOrNull()?.lowercase()?.removePrefix("||") ?: continue
                if (!domain.contains('.')) continue
                if (domain !in seen) {
                    seen.add(domain)
                    if (whitelist.any { domain == it || domain.endsWith(".$it") }) continue
                    if (keep.matches(domain)) continue
                    out.append("0.0.0.0 ").append(domain).append('\n')
                    count++
                }
            }
        }
        return out.toString() to count
    }

    // ── 安装 / 开关 / 卸载 ────────────────────────────────────────────────

    data class InstallResult(val ok: Boolean, val message: String)

    fun install(sourceIds: List<String>, customUrls: List<String>, useMirror: Boolean, whitelist: Set<String>): InstallResult {
        val shell = sh()
        val texts = mutableListOf<String>()
        val urls = SOURCES.filter { it.id in sourceIds }.map { it.url } + customUrls.filter { it.isNotBlank() }
        if (urls.isEmpty()) return InstallResult(false, "未选择任何规则源")
        for (u in urls) {
            val data = download(u, useMirror) ?: return InstallResult(false, "下载失败：$u\n请检查网络或开启 GitHub 加速镜像")
            texts.add(String(data, Charsets.UTF_8))
        }
        val (merged, count) = merge(texts, whitelist)
        if (count < 1000) return InstallResult(false, "合并后仅 $count 条，疑似下载不完整，已放弃安装")

        return try {
            val tmp = File.createTempFile("hosts", ".foxsu")
            tmp.writeText(merged)
            cmd(shell, "mkdir -p $MODULE_DIR/system/etc")
            val (c1, o1) = cmd(
                shell,
                "cp ${escape(tmp.absolutePath)} $MODULE_DIR/system/etc/hosts",
                "chmod 644 $MODULE_DIR/system/etc/hosts",
            )
            tmp.delete()
            if (c1 != 0) return InstallResult(false, "写入失败：${o1.joinToString()}")
            val prop = """
                id=foxsu_adblock
                name=FoxSu 去广告
                version=1.0
                versionCode=1
                author=FoxSu Manager
                description=Systemless hosts 广告屏蔽（由 FoxSu Manager 维护，规则源：开源 hosts 聚合）
            """.trimIndent()
            cmd(
                shell,
                "printf '%s' ${escape(prop)} > $MODULE_DIR/module.prop",
                "rm -f $MODULE_DIR/disable",
                "mkdir -p $WORK_DIR",
                "printf '%s' ${escape(sourceIds.joinToString(","))} > $WORK_DIR/source",
                "printf '%s' ${escape(System.currentTimeMillis().toString())} > $WORK_DIR/updated_at",
                "printf '%s' ${escape(count.toString())} > $WORK_DIR/entries",
                "printf '%s' ${escape(whitelist.joinToString("\n"))} > $WORK_DIR/whitelist",
            )
            InstallResult(true, "已安装 $count 条规则，重启后生效")
        } catch (e: Throwable) {
            Log.e(TAG, "install", e)
            InstallResult(false, "安装失败：${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean): Boolean {
        val shell = sh()
        val r = if (enabled) cmd(shell, "rm -f $MODULE_DIR/disable") else cmd(shell, "touch $MODULE_DIR/disable")
        return r.first == 0
    }

    fun uninstall(): Boolean {
        val shell = sh()
        return cmd(shell, "rm -rf $MODULE_DIR").first == 0
    }

    /** 本机当前 /etc/hosts 的实际条目数（验证是否生效） */
    fun liveEntries(): Int {
        val (_, out) = cmd(sh(), "grep -c '^0\\.0\\.0\\.0' /etc/hosts 2>/dev/null || true")
        return out.firstOrNull()?.trim()?.toIntOrNull() ?: 0
    }

    fun setWhitelist(shell: Shell, domains: List<String>) {
        setCfg(shell, "whitelist", domains.joinToString("\n"))
    }
}
