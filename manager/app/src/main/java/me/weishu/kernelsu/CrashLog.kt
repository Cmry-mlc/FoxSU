package me.weishu.kernelsu

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 极简崩溃落盘：真机上如果启动即崩，logcat 拿不到，就把堆栈留在
 * `Android/data/<pkg>/files/crash/last-crash.txt`（以及应用私有目录同名文件），
 * 下次可以直接用文件管理器或 `adb pull` 取出来定位。
 *
 * 只记录，不吞异常——仍然交给系统默认 handler 走正常的崩溃流程。
 */
internal object CrashLog {

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun write(app: Context, thread: Thread, error: Throwable) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val body = buildString {
            appendLine("time: $stamp")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT})")
            appendLine("thread: ${thread.name}")
            appendLine()
            appendLine(Log.getStackTraceString(error))
        }
        val targets = buildList {
            add(File(app.filesDir, "crash"))
            app.getExternalFilesDir(null)?.let { add(File(it, "crash")) }
        }
        targets.forEach { dir ->
            runCatching {
                if (!dir.exists()) dir.mkdirs()
                File(dir, "last-crash.txt").writeText(body)
            }
        }
    }
}
