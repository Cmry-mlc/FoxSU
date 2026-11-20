package com.kuaimaosu.manager.data.kmroot

import android.content.Context
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 98260 提权引擎 —— 载荷部署 / 配置层（新引擎版）。
 *
 * 引擎为两套 PIE 可执行程序（不是 JNI 库）：
 *   - libkmengine.so  Rust runner，入口 `kmengine run`，负责编排 / 日志转发 / 看门狗；
 *   - libkmplant.so   C plant，实际 UAF 触发 + 写原语 + fake cred + late-load。
 * 外加 kernel_profiles.txt（19 机型偏移档案，运行时按 uname -r 匹配）。
 *
 * 部署：nativeLibraryDir 里的 so + assets 里的 profile 拷到 /data/local/tmp 工作目录，
 * chmod 755 后由 Shizuku shell 域执行。late-load 复用随包 libksud.so（内嵌全套 ko）。
 */
object KmCfg {
    const val DEV_DEFAULT = "/data/local/tmp/kmengine"
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
        arrayOf("libkmengine.so", "kmengine"),
        arrayOf("libkmplant.so", "kmplant"),
        arrayOf("libksud.so", "ksud"),
    )

    private var cachedNativeDir: String? = null
    private var cachedExtDir = ""

    fun runsh() = "$dev/kernel_profiles.txt"

    private fun updateTag(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString(36)
    } catch (_: Throwable) {
        "0"
    }

    /** 探测可写工作目录。优先 /data/local/tmp 子目录，退化到 ND 模式。 */
    fun resolveDev(context: Context?): String {
        ndMode = false
        lastResolveError = ""
        val candidates = arrayOf(DEV_DEFAULT, "/data/local/tmp/kmengine2", "/data/local/tmp")
        for (d in candidates) {
            val r = KmShell.run(
                "mkdir -p '$d' 2>/dev/null;if : > '$d/.km_wtest' 2>/dev/null; then rm -f '$d/.km_wtest'; echo WRITABLE; fi",
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
        val sd = "/sdcard/kmengine"
        val r2 = KmShell.run(
            "mkdir -p '$sd' 2>/dev/null;if : > '$sd/.km_wtest' 2>/dev/null; then rm -f '$sd/.km_wtest'; echo WRITABLE; fi",
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
                    val f = File(ext, "kmengine")
                    f.mkdirs()
                    val path = f.absolutePath
                    val r3 = KmShell.run(
                        "mkdir -p '$path' 2>/dev/null;if : > '$path/.km_wtest' 2>/dev/null; then rm -f '$path/.km_wtest'; echo WRITABLE; fi",
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
        if (!File(d, "libkmengine.so").exists() && File("$d/arm64", "libkmengine.so").exists()) {
            d = "$d/arm64"
        }
        cachedNativeDir = d
        return d
    }

    /** 生成把 native so / profile 拷到工作目录的 shell 命令串。 */
    fun copyScript(context: Context): String {
        val nd = nativeDir(context)
        val d = dev
        val tag = updateTag(context)
        val sb = StringBuilder()
        if (ndMode) {
            sb.append("mkdir -p '$d' 2>&1 | head -n 2;")
            sb.append(appendProfile(context, d))
            sb.append("echo '$tag' > '$d/.deploy_tag' 2>/dev/null;echo DEPLOY_ND tag=$tag nd=$nd")
            return sb.toString()
        }
        sb.append("mkdir -p '$d' 2>&1 | head -n 2;if [ \"\$(cat '$d/.deploy_tag' 2>/dev/null)\" = '$tag' ] && [ -x '$d/kmengine' ] && [ -x '$d/kmplant' ]; then echo DEPLOY_SKIP tag=$tag; else ")
        for (entry in NATIVE_MAP) {
            val src = "$nd/${entry[0]}"
            val dst = "$d/${entry[1]}"
            sb.append("cp -f '$src' '$dst' 2>/dev/null || echo COPY_FAIL ${entry[1]};chmod 755 '$dst' 2>/dev/null;")
        }
        sb.append(appendProfile(context, d))
        sb.append("echo '$tag' > '$d/.deploy_tag'; echo DEPLOY_COPIED tag=$tag; fi;")
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
            "chmod 755 '$dev/ksud' 2>/dev/null;echo KSUD_SOURCE size=\$(stat -c %s '$dev/ksud' 2>/dev/null || echo 0)"
        return KmShell.run(cmd, 30).trim()
    }

    fun mode(context: Context): String {
        val m = context.applicationContext.getSharedPreferences("kmroot", 0).getString("mode", "fake")
        return if (m.isNullOrEmpty()) "fake" else m
    }

    fun setMode(context: Context, value: String?) {
        context.applicationContext.getSharedPreferences("kmroot", 0).edit()
            .putString("mode", value ?: "fake").apply()
    }

    fun extDir(context: Context): String {
        if (cachedExtDir.isNotEmpty()) return cachedExtDir
        var base = context.getExternalFilesDir(null) ?: context.filesDir
        if (!base.isDirectory) base.mkdirs()
        val d = File(base, "kmengine")
        d.mkdirs()
        if (!probeWritable(d)) {
            val alt = File(base, "kmengine-app")
            alt.mkdirs()
            cachedExtDir = (if (probeWritable(alt)) alt else d).absolutePath
            return cachedExtDir
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

    /** 把 kernel_profiles.txt 写到外部目录，并同步到 shell 工作目录。 */
    fun ensureAssets(context: Context): String {
        assetsError = ""
        val dir = extDir(context)
        val tagFile = File(dir, ".assets_tag")
        val tag = updateTag(context)
        val changed = tag != readSmall(tagFile)
        val prof = File(dir, "kernel_profiles.txt")
        var missing = ""
        if (changed || !prof.exists() || prof.length() < 1000) {
            if (!writeAsset(context, "$ASSETS/kernel_profiles.txt", prof)) missing = "kernel_profiles.txt "
        }
        if (missing.isEmpty()) writeSmall(tagFile, tag)
        else assetsError = "写盘失败或内容不完整（${missing.trim()}），目录 $dir" +
            if (lastWriteError.isEmpty()) "" else "；最后一次写入错误：$lastWriteError"
        KmShell.run("mkdir -p '$dev' 2>/dev/null;cp -f '$dir/kernel_profiles.txt' '$dev/kernel_profiles.txt' 2>/dev/null;chmod 644 '$dev/kernel_profiles.txt' 2>/dev/null", 15)
        return dir
    }

    private fun appendProfile(context: Context, d: String): String {
        val b64 = readAssetB64(context, "$ASSETS/kernel_profiles.txt") ?: return ""
        return "printf '%s' '$b64' | base64 -d > '$d/kernel_profiles.txt' 2>/dev/null || echo COPY_FAIL profile;chmod 644 '$d/kernel_profiles.txt' 2>/dev/null;"
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

    private fun readSmall(file: File): String = try {
        if (file.exists() && file.length() <= 4096) {
            val len = file.length().toInt()
            val buf = ByteArray(len)
            FileInputStream(file).use { fis ->
                var i = 0
                while (i < len) { val n = fis.read(buf, i, len - i); if (n > 0) i += n else break }
            }
            String(buf, Charsets.UTF_8).trim()
        } else ""
    } catch (_: Throwable) {
        ""
    }

    private fun writeSmall(file: File, value: String) {
        try {
            FileOutputStream(file, false).use { it.write(value.toByteArray(Charsets.UTF_8)); it.flush() }
        } catch (_: Throwable) {
        }
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

    fun verifyScript(): String {
        val d = dev
        return "echo \"engine:\$(stat -c %s '$d/kmengine' 2>/dev/null || echo 0) plant:\$(stat -c %s '$d/kmplant' 2>/dev/null || echo 0) ksud:\$(stat -c %s '$d/ksud' 2>/dev/null || echo 0) prof:\$(stat -c %s '$d/kernel_profiles.txt' 2>/dev/null || echo 0)\";"
    }
}
