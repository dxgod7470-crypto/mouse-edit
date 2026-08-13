package com.sazanx.mouseconfigurator.shizuku

import rikka.shizuku.Shizuku

object ShizukuShell {
    fun exec(command: String): Boolean {
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
