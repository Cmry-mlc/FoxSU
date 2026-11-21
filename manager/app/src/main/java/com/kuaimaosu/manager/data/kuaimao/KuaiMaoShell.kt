package com.kuaimaosu.manager.data.kuaimao

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * KuaiMao 提权引擎 —— shell 进程层。
 * 通过反射调用 Shizuku 隐藏的 newProcess 拿到 shell 域进程。
 */
object KuaiMaoShell {
    @Volatile
    var lastError: String = ""
        private set

    /** 反射调用 Shizuku.newProcess(String[], String[], String) —— 官方 api 里是 private static。 */
    @Throws(Exception::class)
    private fun newProcess(cmd: Array<String>, env: Array<String>?, dir: String): Process {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        m.isAccessible = true
        return m.invoke(null, cmd, env, dir) as Process
    }

    fun run(cmd: String, timeoutSec: Int, dir: String? = null): String {
        var process: Process? = null
        return try {
            lastError = ""
            val argv = arrayOf("sh", "-c", cmd)
            process = newProcess(argv, null, dir ?: "/data/local/tmp")
            val sb = StringBuilder()
            val t1 = drain(process.inputStream, sb)
            val t2 = drain(process.errorStream, sb)
            val deadline = System.currentTimeMillis() + (Math.max(2, timeoutSec) * 1000L)
            var prevLen = -1
            var stable = 0
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(300)
                val len = synchronized(sb) { sb.length }
                if (len > 0 && len == prevLen) {
                    stable++
                    if (stable >= 5) break
                } else {
                    stable = 0
                }
                if (!t1.isAlive && !t2.isAlive) break
                prevLen = len
            }
            destroyQuietly(process)
            synchronized(sb) { sb.toString() }
        } catch (t: Throwable) {
            var e = t
            while (e is InvocationTargetException && e.cause != null) e = e.cause!!
            lastError = e.toString()
            destroyQuietly(process)
            ""
        }
    }

    @Throws(Exception::class)
    fun spawn(argv: Array<String>, dir: String): Live = Live(newProcess(argv, null, dir))

    private fun destroyQuietly(p: Process?) {
        if (p == null) return
        try {
            p.destroy()
        } catch (_: Throwable) {
        }
    }

    private fun drain(input: InputStream, sb: StringBuilder): Thread {
        val t = Thread {
            try {
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { br ->
                    while (true) {
                        val line = br.readLine() ?: break
                        synchronized(sb) { sb.append(line).append('\n') }
                    }
                }
            } catch (_: Throwable) {
            }
        }
        t.isDaemon = true
        t.start()
        return t
    }

    fun isAlive(p: Process?): Boolean = try {
        p?.isAlive == true
    } catch (_: Throwable) {
        false
    }

    class Live(val proc: Process) {
        private val stdin = proc.outputStream
        private val stopped = AtomicBoolean(false)
        private val lines = AtomicInteger()

        fun isAlive(): Boolean = !stopped.get() && KuaiMaoShell.isAlive(proc)
        fun lineCount(): Int = lines.get()

        fun write(s: String): Boolean = try {
            stdin.write((s + "\n").toByteArray(StandardCharsets.UTF_8))
            stdin.flush()
            true
        } catch (_: Throwable) {
            false
        }

        fun startReaders(stdout: ((String) -> Unit)?, stderr: ((String) -> Unit)?) {
            val t1 = pump(proc.inputStream, stdout)
            val t2 = pump(proc.errorStream, stderr)
            t1.isDaemon = true
            t2.isDaemon = true
            t1.start()
            t2.start()
        }

        private fun pump(input: InputStream, cb: ((String) -> Unit)?): Thread = Thread {
            try {
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { br ->
                    while (true) {
                        val line = br.readLine() ?: break
                        lines.incrementAndGet()
                        if (cb != null) try { cb(line) } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {
            }
        }

        fun kill() {
            stopped.set(true)
            destroyQuietly(proc)
        }
    }
}
