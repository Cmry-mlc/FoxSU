package com.kuaimaosu.manager.data.kmroot

import android.content.Context
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

/**
 * KMRoot 提权引擎 —— 载荷部署 / 配置层。
 * 移植自 com.km.kmroot.core.Cfg，改动：
 *  - 数据文件从 assets/kmroot/ 读取（原为 assets 根）；
 *  - 去掉内置 manager.apk 的部署（改用 KuaiMaoSU 自身管理器，省 ~13MB）；
 *  - nativeLibraryDir 里的 exploit so + 我们自带的 libksud.so 直接落地。
 */
object KmCfg {
    const val DEV_DEFAULT = "/data/local/tmp/kmroot70"
    @Volatile
    var dev: String = DEV_DEFAULT
        private set
    @Volatile
    var ndMode: Boolean = false
        private set
    @Volatile
    var lastResolveError: String = ""
        private set
    @Volatile
    var lastWriteError: String = ""
        private set
    @Volatile
    var assetsError: String = ""
        private set

    private const val ASSETS = "kmroot"

    /** (so 文件名 -> 落地别名) */
    val NATIVE_MAP = arrayOf(
        arrayOf("libkmfast.so", "kmfast"),
        arrayOf("libkmfastnd.so", "kmfastnd"),
        arrayOf("libkmfast61.so", "kmfast61"),
        arrayOf("libkmslow.so", "kmslow"),
        arrayOf("libkmslowsu.so", "kmslowsu"),
        arrayOf("libkm612.so", "km612"),
        arrayOf("libkm612su.so", "km612su"),
        arrayOf("libkm612n.so", "km612n"),
        arrayOf("libkm612x.so", "km612x"),
        arrayOf("libkm612s.so", "km612s"),
        arrayOf("libkmplant.so", "kmplant"),
        arrayOf("libkmsu.so", "kmsu"),
        arrayOf("libkmsolve.so", "kmsolve"),
        arrayOf("libkmfork.so", "kmfork"),
        arrayOf("libksud.so", "ksud"),
        // KMRoot 的这两个 so 与工程自带 CMake 产物同名，故改文件名（落地别名保持原样）
        arrayOf("libkmko.so", "kernelsu.ko"),
        arrayOf("libkmadbroot.so", "libadbroot.so")
    )

    private val ASSET_TEXT = arrayOf("trprofile_pkj110.txt", "trprofile_pkh110.txt")
    private var cachedNativeDir: String? = null
    private var cachedExtDir = ""

    fun runsh() = "$dev/trrun.sh"

    private fun updateTag(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString(36)
    } catch (_: Throwable) {
        "0"
    }

    private fun is66(k: String?) = k != null && k.contains("6.6.")
    private fun is61(k: String?) = k != null && Regex("^6\\.1\\..*").matches(k)
    private fun is612(k: String?): Boolean {
        if (k == null || is66(k)) return false
        return Regex("^6\\.1[2-9].*").matches(k) || Regex("^6\\.[2-9][0-9].*").matches(k)
    }

    /** 探测可写工作目录。优先 /data/local/tmp 子目录，退化到 ND 模式（直接从 nativeLibraryDir 执行）。 */
    fun resolveDev(context: Context?): String {
        ndMode = false
        lastResolveError = ""
        val candidates = arrayOf(DEV_DEFAULT, "/data/local/tmp/kmroot70v2", "/data/local/tmp/kmroot70v3", "/data/local/tmp")
        for (d in candidates) {
            val r = KmShell.run(
                "mkdir -p '$d' 2>/dev/null;if : > '$d/.kmroot_wtest' 2>/dev/null; then rm -f '$d/.kmroot_wtest'; echo WRITABLE; fi",
                10
            )
            if (r.contains("WRITABLE")) {
                dev = d
                if (d == "/data/local/tmp") {
                    lastResolveError = "DEGRADED: 子目录建不出来，直接用 /data/local/tmp 当工作目录"
                }
                return d
            }
        }
        val sd = "/sdcard/kmroot70"
        val r2 = KmShell.run(
            "mkdir -p '$sd' 2>/dev/null;if : > '$sd/.kmroot_wtest' 2>/dev/null; then rm -f '$sd/.kmroot_wtest'; echo WRITABLE; fi",
            10
        )
        if (r2.contains("WRITABLE")) {
            dev = sd
            ndMode = true
            lastResolveError = "ND 模式：/data/local/tmp 不可写，改用 $sd（载荷直接从 nativeLibraryDir 执行）"
            return sd
        }
        if (context != null) {
            try {
                val ext = context.getExternalFilesDir(null)
                if (ext != null) {
                    val f = File(ext, "kmroot70")
                    f.mkdirs()
                    val path = f.absolutePath
                    val r3 = KmShell.run(
                        "mkdir -p '$path' 2>/dev/null;if : > '$path/.kmroot_wtest' 2>/dev/null; then rm -f '$path/.kmroot_wtest'; echo WRITABLE; fi",
                        10
                    )
                    if (r3.contains("WRITABLE")) {
                        dev = path
                        ndMode = true
                        lastResolveError = "ND 模式：改用外部目录 $path（载荷直接从 nativeLibraryDir 执行）"
                        return path
                    }
                }
            } catch (_: Throwable) {
            }
        }
        dev = DEV_DEFAULT
        lastResolveError = "工作目录均不可写，降级用默认目录（可能失败）"
        return dev
    }

    fun nativeDir(context: Context): String {
        cachedNativeDir?.let { return it }
        var d = context.applicationInfo.nativeLibraryDir
        if (!File(d, "libkmfast.so").exists() && File("$d/arm64", "libkmfast.so").exists()) {
            d = "$d/arm64"
        }
        cachedNativeDir = d
        return d
    }

    private fun keepPayload(name: String, kernel: String?, managerInstalled: Boolean): Boolean = when (name) {
        "kmfast61" -> is61(kernel) || is612(kernel)
        "kmfastnd" -> !is612(kernel)
        "km612n", "km612x", "km612" -> !(is66(kernel) || is61(kernel))
        // km612su 是老版遗留（trrun.sh: 新版 km612 单二进制已取代 2.1MB 的 km612su），
        // 原始 APK 里根本没有该 so，落地只会 COPY_FAIL，故永不拷贝。
        "km612su" -> false
        "km612s" -> is612(kernel) == true
        "kmfast" -> !is612(kernel)
        "trsyms_pkh110.txt", "trsyms.txt" -> !is612(kernel)
        "trsyms_612.txt" -> !is66(kernel)
        else -> true // 已去掉 manager.apk 分支；ksud/kernelsu.ko 等一律落地
    }

    /** 生成把 native so / prof 拷到工作目录的 shell 命令串。 */
    fun copyScript(context: Context, kernel: String?, managerInstalled: Boolean): String {
        val nd = nativeDir(context)
        val d = dev
        val tag = "${updateTag(context)}-${(kernel ?: "").hashCode().toString(16)}-m"
        val sb = StringBuilder()
        if (ndMode) {
            sb.append("mkdir -p '$d' 2>&1 | head -n 2;")
            sb.append(appendProfiles(context, d))
            sb.append("echo '$tag' > '$d/.deploy_tag' 2>/dev/null;echo DEPLOY_ND tag=$tag nd=$nd")
            return sb.toString()
        }
        sb.append("mkdir -p '$d' 2>&1 | head -n 2;if [ \"\$(cat '$d/.deploy_tag' 2>/dev/null)\" = '$tag' ] && [ -f '$d/trrun.sh' ] && [ -f '$d/ksud' ]; then echo DEPLOY_SKIP tag=$tag; else ")
        for (entry in NATIVE_MAP) {
            if (keepPayload(entry[1], kernel, managerInstalled)) {
                val dst = "$d/${entry[1]}"
                sb.append("cp -f '$nd/${entry[0]}' '$dst' 2>/dev/null || echo COPY_FAIL ${entry[1]};chmod 755 '$dst' 2>/dev/null;")
            }
        }
        sb.append(appendProfiles(context, d))
        sb.append("chmod 644 '$d/trsyms.txt' '$d/trsyms_pkh110.txt' '$d/trsyms_612.txt' '$d/trprofile_pkj110.txt' '$d/trprofile_pkh110.txt' '$d/trrun.sh' '$d/trslide.sh' '$d/kernelsu.ko' 2>/dev/null;echo '$tag' > '$d/.deploy_tag'; echo DEPLOY_COPIED tag=$tag; fi;")
        return sb.toString()
    }

    /** 从所选管理器 APK 提取 libksud.so（优先），落地为工作目录的 ksud。 */
    fun deployKsud(mgr: String): String {
        val path = KmShell.run("pm path '$mgr' 2>/dev/null | head -n1", 15).trim()
        if (!path.startsWith("package:")) return "KSUD_SOURCE=builtin（$mgr 未安装，用随包 ksud）"
        val apk = path.removePrefix("package:").trim()
        val cmd = "A='$apk';DIR=\$(dirname \"\$A\");M='$dev/ksud.mgr'; rm -f \"\$M\";" +
            "for L in \"\$DIR/lib/arm64/libksud.so\" \"\$DIR/lib/arm64-v8a/libksud.so\"; do if [ -f \"\$L\" ]; then cp -f \"\$L\" \"\$M\" 2>/dev/null; break; fi; done;" +
            "if [ ! -s \"\$M\" ] && command -v unzip >/dev/null 2>&1; then unzip -p \"\$A\" lib/arm64-v8a/libksud.so > \"\$M\" 2>/dev/null; fi;" +
            "if [ -s \"\$M\" ]; then mv -f \"\$M\" '$dev/ksud' 2>/dev/null; echo USED=manager; else rm -f \"\$M\"; echo USED=builtin; fi;" +
            "chmod 755 '$dev/ksud' 2>/dev/null;echo KSUD_SOURCE=\"\$A\" size=\$(stat -c %s '$dev/ksud' 2>/dev/null || echo 0)"
        return KmShell.run(cmd, 30).trim()
    }

    fun mode(context: Context): String {
        val m = context.applicationContext.getSharedPreferences("kmroot", 0).getString("mode", "auto")
        return if (m.isNullOrEmpty()) "auto" else m
    }

    fun setMode(context: Context, value: String?) {
        context.applicationContext.getSharedPreferences("kmroot", 0).edit()
            .putString("mode", value ?: "auto").apply()
    }

    fun extDir(context: Context): String {
        if (cachedExtDir.isNotEmpty()) return cachedExtDir
        var base = context.getExternalFilesDir(null) ?: context.filesDir
        if (!base.isDirectory) base.mkdirs()
        val d = File(base, "kmroot")
        d.mkdirs()
        if (!probeWritable(d)) {
            val stale = File(base, "kmroot.stale.${System.currentTimeMillis()}")
            d.renameTo(stale)
            d.mkdirs()
            if (!probeWritable(d)) {
                val alt = File(base, "kmroot-app")
                alt.mkdirs()
                cachedExtDir = (if (probeWritable(alt)) alt else d).absolutePath
                return cachedExtDir
            }
        }
        cachedExtDir = d.absolutePath
        return cachedExtDir
    }

    private fun probeWritable(d: File): Boolean = try {
        val p = File(d, ".probe")
        FileOutputStream(p, false).use { it.write(107); it.flush() }
        p.delete()
        true
    } catch (t: Throwable) {
        lastWriteError = "$d/.probe → $t"
        false
    }

    /** 把 trrun.sh / trslide.sh / 符号表 / prof 写到外部目录。 */
    fun ensureAssets(context: Context, kernel: String?): String {
        assetsError = ""
        val dir = extDir(context)
        val tagFile = File(dir, ".assets_tag")
        val tag = updateTag(context)
        val changed = tag != readSmall(tagFile)
        val missing = StringBuilder()

        val runsh = File(dir, "trrun.sh")
        if (changed || !runsh.exists() || runsh.length() < 10000) {
            if (!writeAsset(context, "$ASSETS/trrun.sh", runsh)) missing.append("trrun.sh ")
            else stripCrLf(runsh)
        }
        val slide = File(dir, "trslide.sh")
        if (changed || !slide.exists() || slide.length() < 500) {
            if (!writeAsset(context, "$ASSETS/trslide.sh", slide)) missing.append("trslide.sh ")
            else stripCrLf(slide)
        }
        val flushSo = File(dir, "libkmflush.so")
        if (changed || !flushSo.exists() || flushSo.length() < 1000) {
            if (!writeAsset(context, "$ASSETS/libkmflush.so", flushSo) && flushSo.exists()) flushSo.delete()
        }
        val symName = symsFor(kernel)
        val syms = File(dir, symName)
        if (changed || !syms.exists() || syms.length() < 100000) {
            val gz = "$ASSETS/syms/$symName.gz"
            val ok = if (assetExists(context, gz)) gunzipAsset(context, gz, syms)
                     else writeAsset(context, "$ASSETS/syms/$symName", syms)
            if (!ok || syms.length() < 100000) missing.append("$symName ")
        }

        if (missing.isEmpty()) writeSmall(tagFile, tag)
        else assetsError = "写盘失败或内容不完整（${missing.toString().trim()}），目录 $dir" +
            if (lastWriteError.isEmpty()) "" else "；最后一次写入错误：$lastWriteError"
        return dir
    }

    private fun symsFor(kernel: String?): String = when {
        kernel != null && kernel.contains("gc4127a25dcf3") -> "trsyms_pkh110.txt"
        is612(kernel) -> "trsyms_612.txt"
        else -> "trsyms.txt"
    }

    private fun stripCrLf(file: File) {
        try {
            if (!file.exists() || file.length() == 0L || file.length() > 4 * 1024 * 1024) return
            val len = file.length().toInt()
            val buf = ByteArray(len)
            FileInputStream(file).use { fis ->
                var i = 0
                while (i < len) {
                    val n = fis.read(buf, i, len - i)
                    if (n > 0) i += n else break
                }
            }
            if (buf.none { it == 13.toByte() }) return
            val out = ByteArrayOutputStream(len)
            for (b in buf) if (b != 13.toByte()) out.write(b.toInt())
            FileOutputStream(file, false).use { it.write(out.toByteArray()); it.flush() }
        } catch (_: Throwable) {
        }
    }

    private fun readSmall(file: File): String = try {
        if (file.exists() && file.length() <= 4096) {
            val len = file.length().toInt()
            val buf = ByteArray(len)
            FileInputStream(file).use { fis ->
                var i = 0
                while (i < len) { val n = fis.read(buf, i, len - i); if (n > 0) i += n else break }
            }
            String(buf, StandardCharsets.UTF_8).trim()
        } else ""
    } catch (_: Throwable) {
        ""
    }

    private fun writeSmall(file: File, value: String) {
        try {
            FileOutputStream(file, false).use { it.write(value.toByteArray(StandardCharsets.UTF_8)); it.flush() }
        } catch (_: Throwable) {
        }
    }

    private fun assetExists(context: Context, path: String): Boolean = try {
        context.assets.open(path).use { true }
    } catch (_: Throwable) {
        false
    }

    private fun writeAsset(context: Context, path: String, file: File): Boolean = try {
        context.assets.open(path).use { input ->
            FileOutputStream(file).use { out ->
                val buf = ByteArray(8192)
                while (true) { val n = input.read(buf); if (n <= 0) break; out.write(buf, 0, n) }
                out.flush()
            }
        }
        true
    } catch (t: Throwable) {
        lastWriteError = "$path → $t"
        false
    }

    private fun gunzipAsset(context: Context, path: String, file: File): Boolean = try {
        GZIPInputStream(context.assets.open(path)).use { gz ->
            FileOutputStream(file).use { out ->
                val buf = ByteArray(65536)
                while (true) { val n = gz.read(buf); if (n <= 0) break; out.write(buf, 0, n) }
                out.flush()
            }
        }
        true
    } catch (t: Throwable) {
        lastWriteError = "$path → $t"
        false
    }

    private fun appendProfiles(context: Context, d: String): String {
        val sb = StringBuilder()
        for (name in ASSET_TEXT) {
            val b64 = readAssetB64(context, "$ASSETS/prof/$name")
            if (b64 != null) sb.append("printf '%s' '$b64' | base64 -d > '$d/$name' 2>/dev/null || echo COPY_FAIL $name;chmod 644 '$d/$name' 2>/dev/null;")
        }
        for (i in 0..24) {
            val name = "prof_%02d.txt".format(i)
            val b64 = readAssetB64(context, "$ASSETS/prof/$name")
            if (b64 != null) sb.append("printf '%s' '$b64' | base64 -d > '$d/$name' 2>/dev/null || echo COPY_FAIL $name;chmod 644 '$d/$name' 2>/dev/null;")
        }
        return sb.toString()
    }

    private fun readAssetB64(context: Context, path: String): String? = try {
        context.assets.open(path).use { input ->
            val baos = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (true) { val n = input.read(buf); if (n <= 0) break; baos.write(buf, 0, n) }
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }
    } catch (_: Throwable) {
        null
    }

    fun verifyScript(kernel: String?, managerInstalled: Boolean): String {
        val d = dev
        val sb = StringBuilder()
        for (entry in NATIVE_MAP) {
            if (keepPayload(entry[1], kernel, managerInstalled)) {
                sb.append("echo \"size:\$(stat -c %s '$d/${entry[1]}' 2>/dev/null || echo 0) name:$d/${entry[1]}\";")
            }
        }
        return sb.toString()
    }
}
