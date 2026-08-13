package com.sazanx.mouseconfigurator.input

import com.sazanx.mouseconfigurator.model.MouseConfig
import kotlin.math.abs
import kotlin.math.sqrt

data class ProcessedMotion(val dx: Int, val dy: Int)

class InputProcessor {
    private var smoothedX = 0f
    private var smoothedY = 0f
    private var remainderX = 0f
    private var remainderY = 0f

    @Synchronized
    fun process(rawX: Float, rawY: Float, config: MouseConfig): ProcessedMotion {
        var x = rawX * config.pointer * config.x
        var y = rawY * config.pointer * config.y
        if (config.invertY) y = -y

        if (config.acceleration && !config.raw) {
            val magnitude = sqrt(x * x + y * y)
            val boost = 1f + (magnitude / 24f).coerceIn(0f, 1f) * config.accel.coerceIn(0f, 1f)
            x *= boost
            y *= boost
        }

        val curveScale = when (config.curve) {
            "Soft" -> 0.90f + 0.10f * (1f / (1f + abs(x) + abs(y)))
            "Windows-like" -> 0.95f + 0.05f * (1f / (1f + abs(x) + abs(y)))
            "Aggressive" -> 1.0f + 0.10f * (abs(x) + abs(y)).coerceAtMost(1f)
            else -> 1f
        }
        x *= curveScale
        y *= curveScale

        if (!config.raw) {
            val alpha = (1f - config.smoothing).coerceIn(0.05f, 1f)
            smoothedX = alpha * x + (1f - alpha) * smoothedX
            smoothedY = alpha * y + (1f - alpha) * smoothedY
            x = smoothedX
            y = smoothedY
        } else {
            smoothedX = x
            smoothedY = y
        }

        val totalX = x + remainderX
        val totalY = y + remainderY
        val outX = totalX.toInt()
        val outY = totalY.toInt()
        remainderX = totalX - outX
        remainderY = totalY - outY
        return ProcessedMotion(outX, outY)
    }

    @Synchronized
    fun reset() {
        smoothedX = 0f
        smoothedY = 0f
        remainderX = 0f
        remainderY = 0f
    }
}
