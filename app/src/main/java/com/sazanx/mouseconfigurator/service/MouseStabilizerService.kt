package com.sazanx.mouseconfigurator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sazanx.mouseconfigurator.model.MouseConfig

class MouseStabilizerService : Service() {
    private var active = false

    companion object {
        var instance: MouseStabilizerService? = null
        var currentConfig: MouseConfig = MouseConfig()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        active = true
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        active = false
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun isActive(): Boolean = active

    fun processAndInjectInput(dx: Float, dy: Float) {
        // Input processing and injection pipeline
    }

    private fun startInForeground() {
        val channelId = "mouse_stabilizer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mouse Stabilizer Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mouse Configurator")
            .setContentText("Mouse stabilization pipeline running")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)
    }
}
