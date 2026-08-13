package com.sazanx.mouseconfigurator.optimization

enum class OptimizationMode { BALANCED, LOW_LATENCY, BATTERY, THERMAL }

data class OptimizationSettings(
    val enabled: Boolean = true,
    val mode: OptimizationMode = OptimizationMode.BALANCED,
    val adaptiveProcessing: Boolean = true,
    val thermalProtection: Boolean = true,
    val batteryOptimization: Boolean = true,
    val diagnostics: Boolean = true
)

class OptimizationManager {
    @Volatile var settings: OptimizationSettings = OptimizationSettings()

    fun shouldProcessInput(): Boolean = settings.enabled

    fun diagnosticsIntervalMs(): Long = when {
        !settings.diagnostics -> 5000L
        settings.mode == OptimizationMode.LOW_LATENCY -> 2000L
        settings.mode == OptimizationMode.BATTERY -> 5000L
        else -> 3000L
    }
}
