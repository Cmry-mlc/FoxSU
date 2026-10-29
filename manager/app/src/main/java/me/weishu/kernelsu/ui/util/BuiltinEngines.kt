package me.weishu.kernelsu.ui.util

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import com.topjohnwu.superuser.Shell

/**
 * 内置能力（Built-in engines）的探测。
 *
 * 这里的每一项都对应一层真实的运行环境，不做任何「开关一拨就当启用」的假设：
 *
 * | 能力 | 真实依赖 |
 * | --- | --- |
 * | Folk Mount | `ksud` 自带的 overlayfs 挂载引擎（KernelSU 原生就有）；装了 metamodule 时由它接管 |
 * | FolkZygisk | 内核把 zygisk 注入通道暴露给 `ksud`；外置 provider 模块也能提供同一能力 |
 * | Sui | `ksud` 内置的 Shizuku 后端载荷 + 内核侧 binder 注入 |
 * | NoMount | 按 KMI 编进内核的 `nomount` 驱动（`/sys/module/nomount`） |
 *
 * 探测只读，全部走一次 root shell（一次 fork，不轮询），失败就降级成 UNKNOWN，
 * 不猜、不伪造成功。
 */
enum class BuiltinEngine(
    val key: String,
    val label: String,
    val tag: String,
    val summary: String,
    val detail: String,
) {
    FOLK_MOUNT(
        key = "folkmount",
        label = "Folk Mount",
        tag = "MODULE MOUNT",
        summary = "模块挂载引擎",
        detail = "把模块目录按 overlayfs 逐层叠到系统分区上。内置引擎随 ksud 一起工作，" +
            "无需任何额外模块；装了 metamodule 时由 metamodule 接管挂载。",
    ),
    FOLK_ZYGISK(
        key = "folkzygisk",
        label = "FolkZygisk",
        tag = "ZYGISK ENGINE",
        summary = "Zygisk 实现",
        detail = "在 zygote 孵化应用时注入，给模块提供进程级 hook 能力。" +
            "内置引擎需要内核暴露 zygisk 注入通道；外置的 provider 模块也能提供同一能力。",
    ),
    SUI(
        key = "sui",
        label = "Sui",
        tag = "SHIZUKU BACKEND",
        summary = "Shizuku API 后端",
        detail = "让使用 Shizuku API 的应用直接拿到受管的 shell 权限，不必另外安装 Shizuku。" +
            "需要 ksud 内置载荷与内核侧的 binder 注入。",
    ),
    NOMOUNT(
        key = "nomount",
        label = "NoMount",
        tag = "VFS REDIRECT",
        summary = "VFS 路径重定向",
        detail = "不产生任何挂载，由内核直接接管路径解析与目录遍历，因此不会出现在 /proc/mounts 里。" +
            "需要按设备 KMI 预先编进内核的 nomount 驱动。",
    ),
}

/** 单项能力当前的真实状态。 */
enum class EngineState {
    /** 已就绪并且在用。 */
    READY,

    /** 能力存在，但当前没启用（例如装了模块却被禁用）。 */
    OFF,

    /** 运行环境不具备这项能力，界面会明确写出缺什么。 */
    MISSING,

    /** 探测失败（没有 root / 命令被 SELinux 拦），不代表不可用。 */
    UNKNOWN,
}

data class EngineReport(
    val engine: BuiltinEngine,
    val state: EngineState,
    /** 判定依据，直接展示给用户看，避免「黑盒状态」。 */
    val evidence: List<String>,
)

data class BuiltinReport(
    val engines: List<EngineReport>,
    val kernelVersion: String,
    val managerVersion: String,
    val mountBackend: String,
    val raw: String = "",
) {
    val readyCount: Int get() = engines.count { it.state == EngineState.READY }
}

object BuiltinEngines {

    /** 一次 root shell 采集全部证据，避免四次 fork。 */
    private const val PROBE = """
        echo '@@meta'
        [ -e /data/adb/metamodule ] && readlink -f /data/adb/metamodule 2>/dev/null
        echo '@@modules'
        ls /data/adb/modules 2>/dev/null
        echo '@@zygisk'
        ls -d /data/adb/modules/*/zygisk /data/adb/ksu/zygisk* 2>/dev/null
        echo '@@sui'
        ls -d /data/adb/ksu/sui* /data/adb/modules/sui* 2>/dev/null
        echo '@@nomount'
        ls -d /sys/module/nomount /data/adb/ksu/nomount* 2>/dev/null
        echo '@@mounts'
        grep -c overlay /proc/mounts 2>/dev/null
        echo '@@end'
    """

    suspend fun probe(): BuiltinReport = withContext(Dispatchers.IO) {
        val raw = runCatching {
            // 全局 root shell（KsuCli 初始化过默认 builder），一次 exec 采集全部证据
            val r = Shell.cmd(PROBE).exec()
            r.out.joinToString("\n")
        }.getOrElse { "" }

        val sections = parseSections(raw)
        // 没有 @@end 说明脚本根本没跑完（没 root / 被 SELinux 拦）：四项一律标未知，
        // 而不是错误地报「内核未提供」。
        val probeOk = raw.contains("@@end")
        val metamodule = sections["meta"].orEmpty().firstOrNull { it.isNotBlank() }
        val modules = sections["modules"].orEmpty().filter { it.isNotBlank() }
        val zygisk = sections["zygisk"].orEmpty().filter { it.isNotBlank() }
        val sui = sections["sui"].orEmpty().filter { it.isNotBlank() }
        val nomount = sections["nomount"].orEmpty().filter { it.isNotBlank() }
        val overlayCount = sections["mounts"].orEmpty().firstOrNull()?.toIntOrNull()

        val mountBackend = if (metamodule != null) {
            "metamodule · " + metamodule.substringAfterLast('/')
        } else {
            "内置引擎 · overlayfs"
        }

        val reports = listOf(
            // ① Folk Mount：KernelSU 的内置挂载引擎始终存在；有 metamodule 时由它接管
            EngineReport(
                engine = BuiltinEngine.FOLK_MOUNT,
                state = if (probeOk) EngineState.READY else EngineState.UNKNOWN,
                evidence = buildList {
                    add(if (metamodule != null) "挂载后端：metamodule（$metamodule）" else "挂载后端：内置 overlayfs 引擎")
                    if (overlayCount != null) add("/proc/mounts 里的 overlay 挂载：$overlayCount 条")
                    if (modules.isNotEmpty()) add("已启用模块：${modules.size} 个")
                },
            ),
            // ② FolkZygisk：内置通道 or 外置 provider
            EngineReport(
                engine = BuiltinEngine.FOLK_ZYGISK,
                state = when {
                    !probeOk -> EngineState.UNKNOWN
                    zygisk.isNotEmpty() -> EngineState.READY
                    else -> EngineState.MISSING
                },
                evidence = if (!probeOk) {
                    listOf("探测脚本没有跑完，通常是缺少 root 授权")
                } else if (zygisk.isNotEmpty()) {
                    listOf("检测到 zygisk 提供者：${zygisk.first().substringAfterLast('/')}")
                } else {
                    listOf(
                        "未检测到 zygisk 提供者模块",
                        "当前内核未暴露内置 zygisk 注入通道",
                    )
                },
            ),
            // ③ Sui
            EngineReport(
                engine = BuiltinEngine.SUI,
                state = when {
                    !probeOk -> EngineState.UNKNOWN
                    sui.isNotEmpty() -> EngineState.READY
                    else -> EngineState.MISSING
                },
                evidence = if (!probeOk) {
                    listOf("探测脚本没有跑完，通常是缺少 root 授权")
                } else if (sui.isNotEmpty()) {
                    listOf("检测到 Sui 载荷：${sui.first()}")
                } else {
                    listOf("未检测到 Sui 载荷目录（/data/adb/ksu/sui）", "当前内核未提供 binder 注入通道")
                },
            ),
            // ④ NoMount
            EngineReport(
                engine = BuiltinEngine.NOMOUNT,
                state = when {
                    !probeOk -> EngineState.UNKNOWN
                    nomount.isNotEmpty() -> EngineState.READY
                    else -> EngineState.MISSING
                },
                evidence = if (!probeOk) {
                    listOf("探测脚本没有跑完，通常是缺少 root 授权")
                } else if (nomount.isNotEmpty()) {
                    listOf("检测到 NoMount 驱动：${nomount.first()}")
                } else {
                    listOf("未加载 nomount 驱动（/sys/module/nomount 不存在）", "需要按设备 KMI 编进内核")
                },
            ),
        )

        BuiltinReport(
            engines = reports,
            kernelVersion = runCatching { Natives.version }.getOrDefault(0).let { "0x%X".format(it) },
            managerVersion = runCatching { me.weishu.kernelsu.BuildConfig.VERSION_NAME }.getOrDefault(""),
            mountBackend = mountBackend,
            raw = raw,
        )
    }

    private fun parseSections(raw: String): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        var current: String? = null
        raw.lineSequence().forEach { line ->
            val l = line.trim()
            when {
                l.startsWith("@@") -> {
                    val key = l.removePrefix("@@")
                    current = key
                    map.getOrPut(key) { mutableListOf() }
                }
                current != null -> map[current]?.add(l)
            }
        }
        return map
    }

    /** 供「复制诊断」用的一段纯文本。 */
    fun diagnosticText(report: BuiltinReport): String = buildString {
        appendLine("FoxSu 内置能力诊断")
        appendLine("设备：${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android：${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）")
        appendLine("内核：${report.kernelVersion}")
        appendLine("管理器：${report.managerVersion}")
        appendLine("挂载后端：${report.mountBackend}")
        appendLine()
        report.engines.forEach { r ->
            appendLine("[${r.engine.label}] ${stateLabel(r.state)}")
            r.evidence.forEach { appendLine("  · $it") }
        }
    }

    fun stateLabel(state: EngineState): String = when (state) {
        EngineState.READY -> "已就绪"
        EngineState.OFF -> "未启用"
        EngineState.MISSING -> "内核未提供"
        EngineState.UNKNOWN -> "未知"
    }
}
