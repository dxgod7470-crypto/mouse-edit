package com.sazanx.mouseconfigurator.optimization

import android.os.Build
import android.os.PowerManager

class ThermalMonitor(private val powerManager: PowerManager?) {
    fun status(): String {
        return if (powerManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Throttling"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Throttling"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttling"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Throttling"
                else -> "Unknown"
            }
        } else {
            "Optimal"
        }
    }
}
