package com.kuaimaosu.manager.data.kmroot

import android.content.Context

/**
 * KMRoot 提权引擎 —— 管理器选择层。
 * 移植自 com.km.kmroot.core.Mgr，默认管理器改为 KuaiMaoSU 自身。
 *
 * 说明：搬运进 KuaiMaoSU 后，late-load 用本 APK 自带、已内嵌全套 ko 的 ksud，
 * 所以把默认管理器固定为 com.kuaimaosu.manager 即可，其它内置项保留以兼容原 KMRoot 行为。
 */
object KmMgr {
    private const val FILE = "KuaiMaoroot"
    private const val KEY = "rootMgrPkg"
    const val KSU = "me.weishu.kernelsu"
    const val NEXT = "com.rifsxd.ksunext"
    const val BAKA = "com.resukisu.resukisu"
    const val SUKI = "com.sukisu.ultra"
    const val FINA = "me.yuki.fina"
    const val SEVENK = "com.sevenk.core"
    const val KUAIMAO = "com.kuaimaosu.manager"

    /** 默认用自身：KuaiMaoSU 排第一。 */
    val LABELS = arrayOf("KuaiMaoSU", "KernelSU", "KernelSU Next", "BakaSU", "SukiSU Ultra", "FinaSU", "7kimisu")
    val PKGS = arrayOf(KUAIMAO, KSU, NEXT, BAKA, SUKI, FINA, SEVENK)

    private fun sp(context: Context) = context.applicationContext.getSharedPreferences(FILE, 0)

    fun pkg(context: Context): String {
        val s = sp(context).getString(KEY, KUAIMAO)
        return if (s.isNullOrEmpty()) KUAIMAO else s
    }

    fun setPkg(context: Context, value: String?) {
        sp(context).edit().putString(KEY, value ?: KUAIMAO).apply()
    }

    fun index(context: Context): Int = PKGS.indexOf(pkg(context))

    fun isBuiltin(value: String) = PKGS.contains(value)

    fun label(context: Context): String {
        val i = index(context)
        return if (i >= 0) LABELS[i] else appLabel(context, pkg(context))
    }

    fun displayName(context: Context, value: String): String {
        val i = PKGS.indexOf(value)
        return if (i >= 0) LABELS[i] else appLabel(context, value)
    }

    fun labelOf(value: String): String {
        val i = PKGS.indexOf(value)
        return if (i >= 0) LABELS[i] else value
    }

    fun appLabel(context: Context, value: String): String = try {
        val pm = context.packageManager
        val label = pm.getApplicationLabel(pm.getApplicationInfo(value, 0))
        if (!label.isNullOrEmpty()) label.toString() else value
    } catch (_: Throwable) {
        value
    }

    fun installed(context: Context, value: String): Boolean = try {
        context.packageManager.getPackageInfo(value, 0)
        true
    } catch (_: Throwable) {
        false
    }
}
