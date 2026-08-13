package com.sazanx.mouseconfigurator.diagnostics

import android.os.Debug

class PerformanceStats {
    private var lastCpuNs = 0L
    private var lastWallNs = 0L

    @Synchronized
    fun sample(): CpuSample {
        val cpu = Debug.threadCpuTimeNanos()
        val wall = System.nanoTime()
        val cpuPercent = if (lastWallNs == 0L) 0.0 else {
            ((cpu - lastCpuNs).toDouble() / (wall - lastWallNs).toDouble() * 100.0).coerceIn(0.0, 100.0)
        }
        lastCpuNs = cpu
        lastWallNs = wall
        return CpuSample(cpuPercent)
    }

    data class CpuSample(val processThreadCpuPercent: Double)
}
