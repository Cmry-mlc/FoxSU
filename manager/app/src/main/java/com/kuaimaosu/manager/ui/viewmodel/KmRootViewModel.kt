package com.kuaimaosu.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kuaimaosu.manager.data.kmroot.KmCfg
import com.kuaimaosu.manager.data.kmroot.KmEngine
import com.kuaimaosu.manager.data.kmroot.KmMgr
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
        val managerPkg: String = KmMgr.KUAIMAO,
        val managerLabel: String = "KuaiMaoSU",
        val managerInstalled: Boolean = true,
        val mode: String = "auto",
        val running: Boolean = false,
        val pct: Int = 0,
        val stage: String = "准备",
        val logs: List<String> = emptyList(),
        val resultOk: Boolean? = null,
        val resultMessage: String = "",
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

    override fun onCleared() {
        engine.setListener(null)
        super.onCleared()
    }
}
