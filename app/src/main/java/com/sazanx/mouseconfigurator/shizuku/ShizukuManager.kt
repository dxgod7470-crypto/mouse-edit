package com.sazanx.mouseconfigurator.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuManager {
    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return if (isRunning()) {
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        } else false
    }

    fun requestPermission() {
        if (isRunning()) {
            try {
                Shizuku.requestPermission(0)
            } catch (_: Exception) {}
        }
    }

    fun statusText(): String {
        return when {
            !isRunning() -> "Shizuku Service: Not Running"
            !hasPermission() -> "Shizuku Service: Running (Permission Required)"
            else -> "Shizuku Service: Authorized & Ready"
        }
    }
}
