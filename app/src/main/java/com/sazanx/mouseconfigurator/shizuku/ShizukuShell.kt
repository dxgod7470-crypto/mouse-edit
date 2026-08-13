package com.sazanx.mouseconfigurator.shizuku

import rikka.shizuku.Shizuku
import java.io.OutputStream

/**
 * A single long-lived shell owned by Shizuku.
 * This replaces Runtime.exec("sh"), which only created a normal app-UID shell.
 *
 * Note: Shizuku's newProcess API is deprecated in newer Shizuku releases;
 * UserService is the long-term migration path. This project keeps newProcess
 * isolated so it can be replaced later without touching the input pipeline.
 */
class ShizukuShell(private val manager: ShizukuManager) {
    private var process: Process? = null
    private var output: OutputStream? = null

    @Synchronized
    fun start(): Boolean {
        if (!manager.isRunning() || !manager.hasPermission()) return false
        if (process?.isAlive == true && output != null) return true
        return try {
            process = Shizuku.newProcess(arrayOf("sh"), null, null)
            output = process?.outputStream
            process?.isAlive == true
        } catch (_: Throwable) {
            process = null
            output = null
            false
        }
    }

    @Synchronized
    fun write(command: String): Boolean {
        if (!start()) return false
        return try {
            output?.write(command.toByteArray(Charsets.UTF_8))
            output?.flush()
            true
        } catch (_: Throwable) {
            close()
            false
        }
    }

    @Synchronized
    fun close() {
        try { output?.close() } catch (_: Throwable) {}
        try { process?.destroy() } catch (_: Throwable) {}
        output = null
        process = null
    }
}
