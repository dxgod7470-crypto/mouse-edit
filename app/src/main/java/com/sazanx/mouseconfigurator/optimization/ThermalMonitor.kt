package com.sazanx.mouseconfigurator.optimization

import android.os.Build
import android.os.PowerManager

class ThermalMonitor(private val powerManager: PowerManager?) {
    fun status(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || powerManager == null) return "Unavailable"
        return when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "Normal"
            PowerManager.THERMAL_STATUS_LIGHT -> "Light"
            PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
            PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
            else -> "Unknown"
        }
    }

    fun isHot(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || powerManager == null) return false
        return powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE
    }
}
