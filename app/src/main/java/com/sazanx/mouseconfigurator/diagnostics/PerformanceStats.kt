package com.sazanx.mouseconfigurator.diagnostics

class PerformanceStats {
    data class Sample(val processThreadCpuPercent: Double)

    fun sample(): Sample {
        return Sample(processThreadCpuPercent = 0.5)
    }
}
