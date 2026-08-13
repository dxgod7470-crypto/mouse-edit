package com.sazanx.mouseconfigurator.optimization

import android.os.PowerManager

class ThermalMonitor(private val powerManager: PowerManager?) {
    fun status(): String {
        return if (powerManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            when (powerManager.thermalStatus) {
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
