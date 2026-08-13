package com.sazanx.mouseconfigurator.model

data class MouseConfig(
    val pointer: Float = 1.0f,
    val x: Float = 1.0f,
    val y: Float = 1.0f,
    val acceleration: Boolean = false,
    val accel: Float = 0.0f,
    val smoothing: Float = 0.35f,
    val curve: String = "Linear",
    val raw: Boolean = true,
    val scroll: Float = 1.0f,
    val invertY: Boolean = false
)
