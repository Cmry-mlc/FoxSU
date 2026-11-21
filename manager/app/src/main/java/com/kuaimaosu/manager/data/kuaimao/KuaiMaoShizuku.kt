package com.kuaimaosu.manager.data.kuaimao

import android.content.Context
import android.content.Intent
import android.net.Uri
import rikka.shizuku.Shizuku

/**
 * KuaiMao 一键临时提权引擎 —— Shizuku / Stellar shell 域对接层。
 *
 * 按 KuaiMaoSU 包名适配的 Shizuku / Stellar shell 域对接层。
 * 提权引擎依赖 Shizuku/Stellar 提供的普通 shell 域起步，不依赖设备已有 root。
 */
object KuaiMaoShizuku {
    const val PKG_SHIZUKU = "moe.shizuku.privileged.api"
    const val PKG_STELLAR = "roro.stellar.manager"

    enum class State {
        NOT_INSTALLED, NOT_RUNNING, NO_PERMISSION, READY, UNKNOWN
    }

    private fun hasPkg(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Throwable) {
        false
    }

    fun installedShizuku(context: Context) = hasPkg(context, PKG_SHIZUKU)
    fun installedStellar(context: Context) = hasPkg(context, PKG_STELLAR)
    fun installed(context: Context) = installedShizuku(context) || installedStellar(context)

    fun managerPkg(context: Context): String =
        if (!installedShizuku(context) && installedStellar(context)) PKG_STELLAR else PKG_SHIZUKU

    fun providerName(context: Context): String {
        val sh = installedShizuku(context)
        val st = installedStellar(context)
        return when {
            sh && st -> "Shizuku/Stellar"
            st -> "Stellar"
            else -> "Shizuku"
        }
    }

    private fun hasBinder(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun current(context: Context): State {
        val installed = installed(context)
        val binder = hasBinder()
        if (!installed && !binder) return State.NOT_INSTALLED
        if (!binder) return State.NOT_RUNNING
        return try {
            if (Shizuku.isPreV11()) State.UNKNOWN
            else if (Shizuku.checkSelfPermission() == 0) State.READY else State.NO_PERMISSION
        } catch (_: Throwable) {
            State.NOT_RUNNING
        }
    }

    fun ready(context: Context) = current(context) == State.READY

    fun describe(context: Context, state: State): String = when (state) {
        State.NOT_INSTALLED -> "未安装 Shizuku / Stellar（本功能需要它们的 shell 域）"
        State.NOT_RUNNING -> {
            val hint = if (installedStellar(context) && !installedShizuku(context)) {
                "打开 Stellar 启动服务并给 KuaiMaoSU 授权（Stellar 需开启「Shizuku 兼容」，并把它列为兼容应用）"
            } else {
                "打开 Shizuku 点「启动服务」后回来"
            }
            "${providerName(context)} 未就绪：$hint"
        }
        State.NO_PERMISSION -> "${providerName(context)} 未授权"
        State.READY -> "${providerName(context)} 已就绪"
        State.UNKNOWN -> "${providerName(context)} 状态未知"
    }

    /** 等待 shell 服务就绪；必要时自动发起授权请求。via 回调输出进度文案。 */
    fun waitReady(context: Context, timeoutMs: Long, say: (String) -> Unit): Boolean {
        when (val state = current(context)) {
            State.READY -> return true
            State.NOT_INSTALLED -> {
                say("未安装 Shizuku / Stellar 管理器：先装其中一个并启动服务（本功能需要它的 shell 域）。")
                return false
            }
            State.NOT_RUNNING -> say(describe(context, state))
            State.NO_PERMISSION -> {
                say("发起授权请求（弹窗点「允许」）…")
                request(context, say)
            }
            State.UNKNOWN -> {}
        }
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (current(context) == State.READY) {
                say("${providerName(context)} 已授权。")
                return true
            }
            try {
                Thread.sleep(400)
            } catch (_: InterruptedException) {
            }
        }
        return current(context) == State.READY
    }

    fun request(context: Context, say: (String) -> Unit) {
        try {
            if (!installed(context)) {
                say("未安装 Shizuku / Stellar，无法授权。")
            } else if (!hasBinder()) {
                say(describe(context, State.NOT_RUNNING))
                openManager(context)
            } else {
                val listener = object : Shizuku.OnRequestPermissionResultListener {
                    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                        try {
                            @Suppress("DEPRECATION")
                            Shizuku.removeRequestPermissionResultListener(this)
                        } catch (_: Throwable) {
                        }
                        say(if (grantResult == 0) "授权已允许。" else "授权被拒绝，可在管理器里手动开启。")
                    }
                }
                Shizuku.addRequestPermissionResultListener(listener)
                Shizuku.requestPermission(4001)
            }
        } catch (t: Throwable) {
            say("请求授权失败: $t（可到管理器里手动给 KuaiMaoSU 授权）")
            openManager(context)
        }
    }

    fun openManager(context: Context) {
        try {
            var intent = context.packageManager.getLaunchIntentForPackage(managerPkg(context))
            if (intent == null) {
                intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${managerPkg(context)}"))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Throwable) {
        }
    }
}
