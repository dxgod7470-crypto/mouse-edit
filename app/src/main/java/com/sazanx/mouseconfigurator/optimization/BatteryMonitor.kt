package com.sazanx.mouseconfigurator.optimization

import android.content.Context
import android.os.BatteryManager

class BatteryMonitor(context: Context) {
    private val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    fun percent(): Int = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
}
