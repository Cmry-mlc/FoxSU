package me.weishu.kernelsu.ui.util

import android.content.Context
import me.weishu.kernelsu.ksuApp

/**
 * 首次启动引导的持久化标记。
 * 只有「走完 / 跳过」才落盘，版本升级不会重复弹（v1 之后可加版本号强制重看）。
 */
object OnboardingPrefs {
    private const val PREFS = "foxsu_onboarding"
    private const val KEY_SEEN = "seen_v1"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun seen(context: Context): Boolean = runCatching {
        prefs(context).getBoolean(KEY_SEEN, false)
    }.getOrDefault(false)

    fun markSeen(context: Context) {
        runCatching { prefs(context).edit().putBoolean(KEY_SEEN, true).apply() }
    }

    /** ksuApp 可能尚未初始化（Application.onCreate 之前的调用），这里兜底。 */
    fun seen(): Boolean = runCatching { seen(ksuApp) }.getOrDefault(true)
}
