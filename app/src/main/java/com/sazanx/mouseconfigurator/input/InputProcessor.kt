package com.sazanx.mouseconfigurator.input

import com.sazanx.mouseconfigurator.model.MouseConfig

class InputProcessor {
    fun process(dx: Float, dy: Float, config: MouseConfig): Pair<Float, Float> {
        var finalX = dx * config.x * config.pointer
        var finalY = dy * config.y * config.pointer
        if (config.invertY) finalY = -finalY
        return Pair(finalX, finalY)
    }
}
