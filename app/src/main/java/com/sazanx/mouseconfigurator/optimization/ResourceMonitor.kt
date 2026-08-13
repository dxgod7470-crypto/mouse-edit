package com.sazanx.mouseconfigurator.optimization

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

class ResourceMonitor(private val context: Context) {
    fun snapshot(): Snapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val usedAppMb = Debug.getPss() / 1024
        return Snapshot(usedAppMb, info.availMem / (1024 * 1024), info.totalMem / (1024 * 1024))
    }

    data class Snapshot(val appMemoryMb: Long, val availableMemoryMb: Long, val totalMemoryMb: Long)
}
