package com.sazanx.mouseconfigurator.shizuku

import rikka.shizuku.Shizuku

object ShizukuShell {
    fun exec(command: String): Boolean {
        return try {
            val clazz = Shizuku::class.java
            val method = clazz.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
