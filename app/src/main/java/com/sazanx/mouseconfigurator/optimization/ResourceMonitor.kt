package com.sazanx.mouseconfigurator.optimization

import android.app.ActivityManager
import android.content.Context

class ResourceMonitor(private val context: Context) {
    data class MemorySnapshot(val appMemoryMb: Long, val availableMemoryMb: Long, val totalMemoryMb: Long)

    fun snapshot(): MemorySnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val runtime = Runtime.getRuntime()
        val appMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        return MemorySnapshot(
            appMemoryMb = appMem,
            availableMemoryMb = memoryInfo.availMem / (1024 * 1024),
            totalMemoryMb = memoryInfo.totalMem / (1024 * 1024)
        )
    }
}
