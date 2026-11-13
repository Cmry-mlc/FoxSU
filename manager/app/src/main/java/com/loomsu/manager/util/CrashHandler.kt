package com.loomsu.manager.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 启动期崩溃兜底。
 *
 * 把未捕获异常的堆栈写入应用私有目录，下次启动时由 MainActivity 直接展示，
 * 便于在没有 adb 的设备上定位闪退原因。日志会同时写一份到外部私有目录：
 * Android/data/<包名>/files/last_crash.txt
 */
object CrashHandler {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val report = buildReport(appContext, thread, throwable)
            runCatching {
                File(appContext.filesDir, FILE_NAME).writeText(report)
            }
            runCatching {
                appContext.getExternalFilesDir(null)?.let { dir ->
                    File(dir, FILE_NAME).writeText(report)
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String = buildString {
        appendLine("时间: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        appendLine(
            "设备: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} " +
                "(API ${Build.VERSION.SDK_INT})",
        )
        appendLine("包名: ${context.packageName}")
        appendLine("线程: ${thread.name}")
        appendLine()
        appendLine(Log.getStackTraceString(throwable))
    }

    fun read(context: Context): String? = runCatching {
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.readText()
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}
