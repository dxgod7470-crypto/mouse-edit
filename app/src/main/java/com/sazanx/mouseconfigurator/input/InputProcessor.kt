package com.sazanx.mouseconfigurator.input

import com.sazanx.mouseconfigurator.model.MouseConfig
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class InputProcessor {

    fun process(
        dx: Float,
        dy: Float,
        config: MouseConfig
    ): Pair<Float, Float> {

        // Ignore invalid input instead of allowing NaN/Infinity
        // to propagate through the input pipeline.
        if (!dx.isFinite() || !dy.isFinite()) {
            return Pair(0f, 0f)
        }

        var finalX = dx * config.x * config.pointer
        var finalY = dy * config.y * config.pointer

        // Optional acceleration.
        if (config.acceleration && config.accel > 0f) {

            val magnitude =
                kotlin.math.sqrt(
                    dx * dx + dy * dy
                )

            val strength =
                config.accel.coerceIn(0f, 1f)

            // Gentle velocity-based multiplier.
            val multiplier =
                1f + (
                    (magnitude / 10f)
                        .coerceAtMost(1f) * strength
                )

            finalX *= multiplier
            finalY *= multiplier
        }

        // Response curve.
        val curveStrength = when (config.curve) {
            "Soft" -> 0.70f
            "Windows-like" -> 0.85f
            "Aggressive" -> 1.25f
            else -> 1.0f
        }

        if (curveStrength != 1.0f) {
            finalX = applyCurve(finalX, curveStrength)
            finalY = applyCurve(finalY, curveStrength)
        }

        // Simple per-event smoothing.
        //
        // Keep this deliberately conservative. A large smoothing value
        // can make mouse input feel delayed.
        val smoothing =
            config.smoothing.coerceIn(0f, 1f)

        if (smoothing > 0f) {
            val retain = 1f - smoothing * 0.5f
            finalX *= retain
            finalY *= retain
        }

        if (config.invertY) {
            finalY = -finalY
        }

        return Pair(
            finalX.coerceIn(-10000f, 10000f),
            finalY.coerceIn(-10000f, 10000f)
        )
    }

    private fun applyCurve(
        value: Float,
        strength: Float
    ): Float {

        val magnitude = abs(value)

        if (magnitude < 0.0001f) {
            return 0f
        }

        val normalized =
            (magnitude / (magnitude + 10f))
                .coerceIn(0f, 1f)

        val curved =
            normalized.pow(strength)

        return sign(value) *
            (curved * 10f)
    }
}
