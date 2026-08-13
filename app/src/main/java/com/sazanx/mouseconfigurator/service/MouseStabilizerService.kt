package com.sazanx.mouseconfigurator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sazanx.mouseconfigurator.input.InputProcessor
import com.sazanx.mouseconfigurator.model.MouseConfig
import com.sazanx.mouseconfigurator.optimization.OptimizationManager
import com.sazanx.mouseconfigurator.shizuku.ShizukuShell
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MouseStabilizerService : Service() {
    companion object {
        @Volatile var instance: MouseStabilizerService? = null
        @Volatile var currentConfig: MouseConfig = MouseConfig()
    }

    private val processor = InputProcessor()
    private val optimizer = OptimizationManager()
    private val outputExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var shell: ShizukuShell
    private val running = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        instance = this
        shell = ShizukuShell(com.sazanx.mouseconfigurator.shizuku.ShizukuManager())
        startForegroundNotification()
        running.set(shell.start())
    }

    fun processAndInjectInput(rawDeltaX: Float, rawDeltaY: Float) {
        if (!running.get() || !optimizer.shouldProcessInput()) return
        val motion = processor.process(rawDeltaX, rawDeltaY, currentConfig)
        if (motion.dx == 0 && motion.dy == 0) return

        // Queue output on one worker. No coroutine/job is created per input event.
        outputExecutor.execute {
            if (!running.get()) return@execute
            shell.write("input swipe 0 0 ${motion.dx} ${motion.dy} 1\n")
        }
    }

    fun isActive(): Boolean = running.get()

    private fun startForegroundNotification() {
        val channelId = "mouse_stabilizer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Mouse Precision Service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mouse Configurator")
            .setContentText("Low-overhead input pipeline active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        running.set(false)
        instance = null
        outputExecutor.shutdownNow()
        try { shell.close() } catch (_: Throwable) {}
        processor.reset()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
