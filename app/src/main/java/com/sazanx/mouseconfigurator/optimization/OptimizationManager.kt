package com.sazanx.mouseconfigurator.optimization

class OptimizationManager {
    data class Settings(
        val adaptiveProcessing: Boolean = true,
        val thermalProtection: Boolean = true,
        val batteryOptimization: Boolean = false,
        val diagnostics: Boolean = true
    )

    val settings = Settings()
}
