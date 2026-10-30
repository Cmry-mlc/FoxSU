plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 31
extra["androidTargetSdkVersion"] = 37
extra["androidCompileSdkVersion"] = 37
extra["androidCompileSdkVersionMinor"] = 0
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidCompileNdkVersion"] = libs.versions.ndk.get()
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
extra["managerVersionCode"] = getVersionCode()
extra["managerVersionName"] = getVersionName()

fun getGitCommitCount(): Int {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
    return process.inputStream.bufferedReader().use { it.readText().trim().toInt() }
}

fun getGitDescribe(): String {
    val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always"))
    return process.inputStream.bufferedReader().use { it.readText().trim() }
}

/**
 * 版本号统一从仓库根的 `VERSION` 文件读（形如 `3.0.48`）。
 *
 * 原来的规则是「30000 + git rev-list --count」，这个数字不可复现：
 * 本地仓库是浅克隆（43 个提交），CI 里 checkout 的 fetch-depth: 0 是完整
 * 历史（2977 个提交），同一个 commit 在两边算出来的 versionCode 差了三千多。
 * 结果就是 CI 出的包（32977）装上去之后，本地出的包（30043）再也装不上，
 * 一装就报「应用未安装 / 版本降级」。
 *
 * 现在版本号只由这个文件决定，与克隆深度、commit 数、tag 全部无关。
 */
fun readVersionFile(): String? {
    val candidates = listOf(file("../VERSION"), file("VERSION"))
    val f = candidates.firstOrNull { it.isFile } ?: return null
    val raw = f.readText().trim()
    return if (Regex("^\\d+\\.\\d+\\.\\d+$").matches(raw)) raw else null
}

fun getVersionCode(): Int {
    val v = readVersionFile()
    if (v != null) {
        val (major, minor, patch) = v.split(".").map { it.toInt() }
        // 3.0.48 -> 30048，与旧规则同为 5 位数，可平滑覆盖安装
        return major * 10000 + minor * 100 + patch
    }
    return 30000 + getGitCommitCount()
}

fun getVersionName(): String {
    return readVersionFile() ?: getGitDescribe()
}
