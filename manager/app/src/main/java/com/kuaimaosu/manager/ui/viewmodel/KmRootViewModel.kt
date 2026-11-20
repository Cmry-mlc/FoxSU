package com.kuaimaosu.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kuaimaosu.manager.data.kmroot.KmCfg
import com.kuaimaosu.manager.data.kmroot.KmEngine
import com.kuaimaosu.manager.data.kmroot.KmMgr
import com.kuaimaosu.manager.data.kmroot.KmShell
import com.kuaimaosu.manager.data.kmroot.KmShizuku
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 一键临时提权（KMRoot 引擎）页面状态。
 */
class KmRootViewModel(app: Application) : AndroidViewModel(app) {
    data class UiState(
        val shizukuState: KmShizuku.State = KmShizuku.State.UNKNOWN,
        val shizukuText: String = "",
        val kernel: String = "",
        val managerPkg: String = KmMgr.KUAIMAO,
        val managerLabel: String = "KuaiMaoSU",
        val managerInstalled: Boolean = true,
        val mode: String = "fake",
        val running: Boolean = false,
        val pct: Int = 0,
        val stage: String = "准备",
        val logs: List<String> = emptyList(),
        val resultOk: Boolean? = null,
        val resultMessage: String = "",
        val showSuccessDialog: Boolean = false,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val engine = KmEngine()

    init {
        engine.setListener(object : KmEngine.Listener {
            override fun onLog(line: String) {
                appendLog(line)
            }

            override fun onStage(pct: Int, label: String) {
                _ui.value = _ui.value.copy(pct = pct, stage = label)
            }

            override fun onFinished(ok: Boolean, message: String) {
                _ui.value = _ui.value.copy(
                    running = false,
                    resultOk = ok,
                    resultMessage = message,
                    pct = if (ok) 100 else _ui.value.pct,
                    // 提权成功弹「需重启」说明；失败不弹，让用户直接看日志
                    showSuccessDialog = ok,
                )
                appendLog("== $message")
            }
        })
        refresh()
    }

    fun refresh() {
        val ctx = getApplication<Application>()
        val state = KmShizuku.current(ctx)
        val pkg = KmMgr.pkg(ctx)
        _ui.value = _ui.value.copy(
            shizukuState = state,
            shizukuText = KmShizuku.describe(ctx, state),
            managerPkg = pkg,
            managerLabel = KmMgr.displayName(ctx, pkg),
            managerInstalled = KmMgr.installed(ctx, pkg),
            mode = KmCfg.mode(ctx),
            running = engine.running,
        )
        loadKernel()
    }

    /** 读取当前内核版本串，供"支持机型"弹层高亮匹配项。 */
    fun loadKernel() {
        viewModelScope.launch {
            val k = try {
                KmShell.run("uname -r", 8).trim().lines().firstOrNull()?.trim().orEmpty()
            } catch (_: Throwable) { "" }
            if (k.isNotEmpty()) _ui.value = _ui.value.copy(kernel = k)
        }
    }

    fun selectManager(pkg: String) {
        val ctx = getApplication<Application>()
        KmMgr.setPkg(ctx, pkg)
        refresh()
    }

    fun setMode(mode: String) {
        val ctx = getApplication<Application>()
        KmCfg.setMode(ctx, mode)
        refresh()
    }

    fun requestPermission() {
        KmShizuku.request(getApplication()) { appendLog(it) }
    }

    fun openShizukuManager() {
        KmShizuku.openManager(getApplication())
    }

    private fun appendLog(line: String) {
        val cur = _ui.value.logs.toMutableList()
        cur.add(line)
        while (cur.size > 400) cur.removeAt(0)
        _ui.value = _ui.value.copy(logs = cur)
    }

    fun start() {
        if (_ui.value.running) return
        _ui.value = _ui.value.copy(
            running = true,
            resultOk = null,
            resultMessage = "",
            pct = 0,
            stage = "准备",
            logs = emptyList(),
        )
        engine.start(getApplication())
    }

    fun stop() {
        engine.stop()
        _ui.value = _ui.value.copy(running = false)
    }

    /** 弹窗「稍后手动重启」：仅关闭提示，用户自己决定何时重启。 */
    fun dismissSuccess() {
        _ui.value = _ui.value.copy(showSuccessDialog = false)
    }

    /** 清除一次提权的结果态（供主页内嵌按钮的失败提示「关闭」用）。 */
    fun dismissResult() {
        _ui.value = _ui.value.copy(resultOk = null, resultMessage = "", showSuccessDialog = false)
    }

    /**
     * 弹窗「立即重启 KuaiMaoSU」：先拉起自身 launcher Activity，再结束当前进程，
     * 让 APP 以提权后的 root 环境重新初始化，状态才会正确显示。
     */
    fun restartApp() {
        val ctx = getApplication<Application>()
        try {
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (intent != null) ctx.startActivity(intent)
        } catch (_: Throwable) {
        }
        // 结束当前进程，让新启动的实例在 root 环境下重新初始化
        try {
            Runtime.getRuntime().exit(0)
        } catch (_: Throwable) {
        }
    }

    override fun onCleared() {
        engine.setListener(null)
        super.onCleared()
    }
}
