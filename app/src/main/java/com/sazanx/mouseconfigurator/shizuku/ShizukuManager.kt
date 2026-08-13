package com.sazanx.mouseconfigurator.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuManager {
    companion object { const val PERMISSION_CODE = 1004 }

    fun isRunning(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun hasPermission(): Boolean = try {
        isRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun requestPermission() {
        if (isRunning() && !hasPermission()) {
            Shizuku.requestPermission(PERMISSION_CODE)
        }
    }

    fun statusText(): String = when {
        !isRunning() -> "Shizuku: not running"
        hasPermission() -> "Shizuku: running • granted"
        else -> "Shizuku: running • permission required"
    }
}
