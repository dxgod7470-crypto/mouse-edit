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
import com.sazanx.mouseconfigurator.model.MouseConfig
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class MouseStabilizerService : Service() {

    private var active = false

    /*
     * Last processed movement.
     *
     * These values are useful for diagnostics and testing.
     * They represent the result AFTER the configured pipeline.
     */
    @Volatile
    var lastProcessedX: Float = 0f
        private set

    @Volatile
    var lastProcessedY: Float = 0f
        private set

    @Volatile
    var totalProcessedEvents: Long = 0L
        private set

    @Volatile
    var processingTimeNs: Long = 0L
        private set

    /*
     * Previous output used by the optional smoothing stage.
     */
    private var smoothX = 0f
    private var smoothY = 0f

    companion object {

        @Volatile
        var instance: MouseStabilizerService? = null

        @Volatile
        var currentConfig: MouseConfig = MouseConfig()
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        active = true

        startInForeground()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        return START_STICKY
    }

    override fun onDestroy() {

        active = false
        instance = null

        lastProcessedX = 0f
        lastProcessedY = 0f
        smoothX = 0f
        smoothY = 0f

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    fun isActive(): Boolean = active

    /**
     * Processes one mouse movement through the configured pipeline.
     *
     * Pipeline:
     *
     * raw movement
     *      ↓
     * X/Y sensitivity
     *      ↓
     * optional acceleration
     *      ↓
     * response curve
     *      ↓
     * optional smoothing
     *      ↓
     * processed movement
     *
     * This method intentionally does NOT pretend to inject globally.
     * Android does not provide a normal app-level global mouse injection
     * API through this class.
     */
    fun processAndInjectInput(
        dx: Float,
        dy: Float
    ) {

        if (!active) {
            return
        }

        val startNs = System.nanoTime()

        try {

            val config = currentConfig

            /*
             * Ignore invalid floating-point input.
             */
            if (
                dx.isNaN() ||
                dx.isInfinite() ||
                dy.isNaN() ||
                dy.isInfinite()
            ) {
                return
            }

            /*
             * Start with the raw event.
             *
             * "Raw" here means that we don't apply Android-style
             * acceleration or smoothing ourselves.
             */
            var x = dx
            var y = dy

            /*
             * Sensitivity.
             *
             * Pointer is treated as the global multiplier.
             * X/Y then provide independent axis control.
             */
            x *= config.pointer * config.x
            y *= config.pointer * config.y

            /*
             * Optional acceleration.
             *
             * Acceleration is based on movement magnitude rather than
             * event frequency. This keeps small movements relatively
             * controlled while allowing larger movements to become faster.
             */
            if (config.acceleration) {

                val strength =
                    config.accel.coerceIn(0f, 1f)

                val magnitude =
                    hypot(x, y)

                if (magnitude > 0f) {

                    /*
                     * Normalized acceleration factor.
                     *
                     * The exponent stays conservative so the default
                     * configuration does not become overly aggressive.
                     */
                    val accelerationFactor =
                        1f +
                            strength *
                            (magnitude.pow(0.5f).coerceAtMost(3f))

                    x *= accelerationFactor
                    y *= accelerationFactor
                }
            }

            /*
             * Response curve.
             */
            val curvedX =
                applyCurve(
                    x,
                    config.curve
                )

            val curvedY =
                applyCurve(
                    y,
                    config.curve
                )

            x = curvedX
            y = curvedY

            /*
             * Optional smoothing.
             *
             * 0.0 = completely disabled.
             * Higher values blend more strongly toward the previous
             * processed output.
             */
            val smoothing =
                config.smoothing.coerceIn(0f, 1f)

            if (smoothing > 0f) {

                val currentWeight =
                    1f - smoothing

                smoothX =
                    smoothX * smoothing +
                        x * currentWeight

                smoothY =
                    smoothY * smoothing +
                        y * currentWeight

                x = smoothX
                y = smoothY

            } else {

                /*
                 * Keep the smoothing state synchronized with the
                 * current output when smoothing is disabled.
                 */
                smoothX = x
                smoothY = y
            }

            /*
             * Invert Y is applied last so it is independent of the
             * acceleration/curve calculations.
             */
            if (config.invertY) {
                y = -y
            }

            /*
             * Prevent NaN/Infinity from escaping the pipeline.
             */
            if (
                x.isNaN() ||
                x.isInfinite() ||
                y.isNaN() ||
                y.isInfinite()
            ) {
                return
            }

            /*
             * Store the final processed movement.
             *
             * MainActivity can use these values for diagnostics.
             */
            lastProcessedX = x
            lastProcessedY = y

            totalProcessedEvents++

        } finally {

            processingTimeNs =
                System.nanoTime() - startNs
        }
    }

    /**
     * Applies the selected response curve.
     */
    private fun applyCurve(
        value: Float,
        curve: String
    ): Float {

        if (value == 0f) {
            return 0f
        }

        val direction =
            sign(value)

        val magnitude =
            abs(value)

        return when (curve) {

            "Linear" -> {
                value
            }

            "Soft" -> {
                direction *
                    magnitude.pow(0.85f)
            }

            "Windows-like" -> {
                direction *
                    (
                        magnitude *
                            (
                                0.75f +
                                    0.25f *
                                    magnitude.coerceAtMost(1f)
                            )
                        )
            }

            "Aggressive" -> {
                direction *
                    magnitude.pow(1.15f)
            }

            else -> {
                value
            }
        }
    }

    /**
     * Lightweight hypotenuse calculation.
     */
    private fun hypot(
        x: Float,
        y: Float
    ): Float {

        return kotlin.math.sqrt(
            x * x +
                y * y
        )
    }

    private fun startInForeground() {

        val channelId =
            "mouse_stabilizer_channel"

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    channelId,
                    "Mouse Stabilizer Service",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            )?.createNotificationChannel(
                channel
            )
        }

        val notification: Notification =
            NotificationCompat.Builder(
                this,
                channelId
            )
                .setContentTitle(
                    "Mouse Configurator"
                )
                .setContentText(
                    "Mouse input pipeline running"
                )
                .setSmallIcon(
                    android.R.drawable.stat_notify_sync
                )
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .setOngoing(true)
                .build()

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val serviceType =
                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ) {

                        ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE

                    } else {

                        0
                    }

                startForeground(
                    1,
                    notification,
                    serviceType
                )

            } else {

                startForeground(
                    1,
                    notification
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}
