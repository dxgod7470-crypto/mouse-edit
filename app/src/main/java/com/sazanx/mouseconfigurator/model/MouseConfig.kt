package com.sazanx.mouseconfigurator.model

data class MouseConfig(
    val pointer: Float = 1.0f,
    val x: Float = 1.0f,
    val y: Float = 1.0f,
    val raw: Boolean = true,
    val acceleration: Boolean = false,
    val accel: Float = 0.0f,
    val smoothing: Float = 0.0f,
    val curve: String = "Linear",
    val scroll: Float = 1.0f,
    val invertY: Boolean = false
)
