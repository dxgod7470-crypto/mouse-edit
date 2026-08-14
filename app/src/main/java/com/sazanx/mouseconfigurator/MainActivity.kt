package com.sazanx.mouseconfigurator

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.sazanx.mouseconfigurator.diagnostics.PerformanceStats
import com.sazanx.mouseconfigurator.input.InputEventStats
import com.sazanx.mouseconfigurator.model.MouseConfig
import com.sazanx.mouseconfigurator.optimization.BatteryMonitor
import com.sazanx.mouseconfigurator.optimization.OptimizationManager
import com.sazanx.mouseconfigurator.optimization.ResourceMonitor
import com.sazanx.mouseconfigurator.optimization.ThermalMonitor
import com.sazanx.mouseconfigurator.service.MouseStabilizerService
import com.sazanx.mouseconfigurator.shizuku.ShizukuManager
import java.util.Locale

class MainActivity : Activity() {

    private var cfg = MouseConfig()

    private val shizuku = ShizukuManager()
    private val inputStats = InputEventStats()
    private val performance = PerformanceStats()
    private val optimizationManager = OptimizationManager()

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var status: TextView
    private lateinit var log: TextView

    private var statsTimer: Runnable? = null
    private var lastNs = 0L

    private val thermalMonitor by lazy {
        ThermalMonitor(
            getSystemService(PowerManager::class.java)
        )
    }

    private val batteryMonitor by lazy {
        BatteryMonitor(this)
    }

    private val resourceMonitor by lazy {
        ResourceMonitor(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
    }

    override fun onResume() {
        super.onResume()

        if (::status.isInitialized) {
            updateShizukuStatus()
        }
    }

    override fun onPause() {
        stopStatsTimer()
        super.onPause()
    }

    override fun onDestroy() {
        stopStatsTimer()
        super.onDestroy()
    }

    private fun updateShizukuStatus() {
        if (!::status.isInitialized) {
            return
        }

        try {
            status.text = shizuku.statusText()
        } catch (e: Throwable) {
            status.text = "Shizuku status unavailable"
        }
    }

    private fun buildUi() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                28,
                24,
                28,
                20
            )

            setBackgroundColor(
                Color.rgb(11, 15, 20)
            )
        }

        val title = TextView(this).apply {
            text = "Mouse Configurator v5"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        root.addView(title)

        status = TextView(this).apply {
            text = "Checking Shizuku..."
            textSize = 13f
            setTextColor(Color.LTGRAY)
        }

        root.addView(status)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }

        val startButton = Button(this).apply {
            text = "START"

            setOnClickListener {
                startMouseService()
            }
        }

        controls.addView(
            startButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val stopButton = Button(this).apply {
            text = "STOP"

            setOnClickListener {
                stopMouseService()
            }
        }

        controls.addView(
            stopButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        root.addView(controls)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val buttons = listOf(
            "PC SETTINGS",
            "INPUT TEST",
            "DEVICES",
            "OPTIMIZE"
        )

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(body)
        }

        buttons.forEach { name ->

            val button = Button(this).apply {
                text = name
            }

            tabs.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }

        root.addView(tabs)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        fun show(page: () -> Unit) {

            stopStatsTimer()

            body.removeAllViews()

            try {
                page()
            } catch (e: Throwable) {
                addHeader(body, "Page error")
                addText(
                    body,
                    e.localizedMessage ?: "Unknown error"
                )
            }
        }

        fun configPage() = show {

            addHeader(
                body,
                "PC-style mouse behavior"
            )

            addSeek(
                body,
                "Pointer speed",
                0.1f,
                3f,
                cfg.pointer
            ) {
                cfg = cfg.copy(pointer = it)
                pushConfig()
            }

            addSeek(
                body,
                "X sensitivity",
                0.1f,
                3f,
                cfg.x
            ) {
                cfg = cfg.copy(x = it)
                pushConfig()
            }

            addSeek(
                body,
                "Y sensitivity",
                0.1f,
                3f,
                cfg.y
            ) {
                cfg = cfg.copy(y = it)
                pushConfig()
            }

            addSwitch(
                body,
                "Raw input mode",
                cfg.raw
            ) {
                cfg = cfg.copy(raw = it)
                pushConfig()
            }

            addSwitch(
                body,
                "Mouse acceleration",
                cfg.acceleration
            ) {
                cfg = cfg.copy(acceleration = it)
                pushConfig()
            }

            addSeek(
                body,
                "Acceleration strength",
                0f,
                1f,
                cfg.accel
            ) {
                cfg = cfg.copy(accel = it)
                pushConfig()
            }

            addSeek(
                body,
                "Smoothing / filter",
                0f,
                1f,
                cfg.smoothing
            ) {
                cfg = cfg.copy(smoothing = it)
                pushConfig()
            }

            addHeader(
                body,
                "Response curve"
            )

            val radioGroup = RadioGroup(this)

            listOf(
                "Linear",
                "Soft",
                "Windows-like",
                "Aggressive"
            ).forEach { name ->

                val radio = RadioButton(this).apply {

                    text = name

                    setTextColor(Color.WHITE)

                    isChecked = name == cfg.curve

                    setOnClickListener {

                        cfg = cfg.copy(
                            curve = name
                        )

                        pushConfig()
                    }
                }

                radioGroup.addView(radio)
            }

            body.addView(radioGroup)

            addSeek(
                body,
                "Scroll speed",
                0.25f,
                3f,
                cfg.scroll
            ) {
                cfg = cfg.copy(scroll = it)
                pushConfig()
            }

            addSwitch(
                body,
                "Invert Y",
                cfg.invertY
            ) {
                cfg = cfg.copy(invertY = it)
                pushConfig()
            }

            addText(
                body,
                "The input pipeline is optimized to avoid creating a coroutine for every mouse event."
            )
        }

        fun testPage() = show {

            addHeader(
                body,
                "Raw input tester"
            )

            addText(
                body,
                "Move the mouse over this screen. Events reaching this app appear below."
            )

            log = TextView(this).apply {

                text = "Waiting for mouse/keyboard..."

                setTextColor(
                    Color.rgb(180, 220, 255)
                )

                textSize = 12f
            }

            body.addView(log)
        }

        fun devicePage() = show {

            addHeader(
                body,
                "Connected Input Devices"
            )

            try {

                val deviceIds =
                    InputDevice.getDeviceIds()

                for (id in deviceIds) {

                    val device =
                        InputDevice.getDevice(id)

                    if (
                        device != null &&
                        (
                            device.sources and
                                (
                                    InputDevice.SOURCE_MOUSE or
                                        InputDevice.SOURCE_KEYBOARD
                                )
                        ) != 0
                    ) {

                        addText(
                            body,
                            "${device.name} (ID ${device.id}) • sources 0x${
                                Integer.toHexString(device.sources)
                            }"
                        )
                    }
                }

            } catch (e: Throwable) {

                addText(
                    body,
                    "Unable to read input devices."
                )
            }

            addHeader(
                body,
                "Shizuku"
            )

            val shizukuStatus =
                try {
                    shizuku.statusText()
                } catch (e: Throwable) {
                    "Shizuku unavailable"
                }

            addText(
                body,
                shizukuStatus
            )

            try {

                if (
                    shizuku.isRunning() &&
                    !shizuku.hasPermission()
                ) {

                    body.addView(
                        Button(this).apply {

                            text = "Request Shizuku permission"

                            setOnClickListener {

                                try {

                                    shizuku.requestPermission()

                                    updateShizukuStatus()

                                } catch (e: Throwable) {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Unable to request Shizuku permission",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }

            } catch (e: Throwable) {
                // Ignore Shizuku state errors on this page.
            }
        }

        fun optimizationPage() = show {

            addHeader(
                body,
                "Optimization"
            )

            addText(
                body,
                "These controls reduce this app's own workload."
            )

            val current =
                optimizationManager.settings

            val manager =
                MouseStabilizerService.instance

            addText(
                body,
                "Service: ${
                    if (manager?.isActive() == true)
                        "active"
                    else
                        "stopped"
                }"
            )

            try {

                addText(
                    body,
                    "Thermal: ${thermalMonitor.status()}"
                )

            } catch (e: Throwable) {

                addText(
                    body,
                    "Thermal: unavailable"
                )
            }

            try {

                addText(
                    body,
                    "Battery: ${batteryMonitor.percent()}%"
                )

            } catch (e: Throwable) {

                addText(
                    body,
                    "Battery: unavailable"
                )
            }

            try {

                val mem =
                    resourceMonitor.snapshot()

                addText(
                    body,
                    "App memory: ${mem.appMemoryMb} MB • " +
                        "Available RAM: ${mem.availableMemoryMb} MB / " +
                        "${mem.totalMemoryMb} MB"
                )

            } catch (e: Throwable) {

                addText(
                    body,
                    "Memory information unavailable"
                )
            }

            try {

                val cpu =
                    performance.sample()
                        .processThreadCpuPercent

                addText(
                    body,
                    "UI thread CPU sample: ${
                        String.format(
                            Locale.US,
                            "%.1f",
                            cpu
                        )
                    }%"
                )

            } catch (e: Throwable) {

                addText(
                    body,
                    "CPU information unavailable"
                )
            }

            try {

                addText(
                    body,
                    "Input events: ${
                        inputStats.snapshot().totalEvents
                    }"
                )

            } catch (e: Throwable) {

                addText(
                    body,
                    "Input statistics unavailable"
                )
            }

            addSwitch(
                body,
                "Adaptive processing",
                current.adaptiveProcessing
            ) {}

            addSwitch(
                body,
                "Thermal protection",
                current.thermalProtection
            ) {}

            addSwitch(
                body,
                "Battery optimization",
                current.batteryOptimization
            ) {}

            addSwitch(
                body,
                "Detailed diagnostics",
                current.diagnostics
            ) {}

            startStatsTimer()
        }

        val pages = listOf<() -> Unit>(
            { configPage() },
            { testPage() },
            { devicePage() },
            { optimizationPage() }
        )

        buttons.indices.forEach { index ->

            tabs.getChildAt(index)
                .setOnClickListener {

                    pages[index].invoke()
                }
        }

        configPage()

        updateShizukuStatus()
    }

    private fun pushConfig() {

        try {

            MouseStabilizerService.currentConfig =
                cfg

        } catch (e: Throwable) {

            Toast.makeText(
                this,
                "Unable to apply configuration",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addHeader(
        parent: LinearLayout,
        textValue: String
    ) {

        parent.addView(
            TextView(this).apply {

                text = textValue

                textSize = 19f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(Color.WHITE)

                setPadding(
                    0,
                    18,
                    0,
                    8
                )
            }
        )
    }

    private fun addText(
        parent: LinearLayout,
        textValue: String
    ) {

        parent.addView(
            TextView(this).apply {

                text = textValue

                setTextColor(Color.LTGRAY)

                textSize = 13f

                setPadding(
                    0,
                    6,
                    0,
                    6
                )
            }
        )
    }

    private fun addSeek(
        parent: LinearLayout,
        label: String,
        minValue: Float,
        maxValue: Float,
        initial: Float,
        change: (Float) -> Unit
    ) {

        val labelView =
            TextView(this).apply {

                setTextColor(Color.WHITE)

                textSize = 14f
            }

        val seekBar =
            SeekBar(this).apply {

                max = 1000
            }

        fun updateProgress(
            progressValue: Int
        ) {

            val calculatedValue =
                minValue +
                    (maxValue - minValue) *
                    (progressValue / 1000f)

            labelView.text =
                "$label  ${
                    String.format(
                        Locale.US,
                        "%.2f",
                        calculatedValue
                    )
                }"

            change(calculatedValue)
        }

        val initialProgress =
            (
                (initial - minValue) /
                    (maxValue - minValue) *
                    1000f
                )
                .toInt()
                .coerceIn(0, 1000)

        seekBar.progress =
            initialProgress

        seekBar.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBarWidget: SeekBar?,
                    progressValue: Int,
                    fromUser: Boolean
                ) {

                    if (fromUser) {
                        updateProgress(progressValue)
                    }
                }

                override fun onStartTrackingTouch(
                    seekBarWidget: SeekBar?
                ) {}

                override fun onStopTrackingTouch(
                    seekBarWidget: SeekBar?
                ) {}
            }
        )

        labelView.text =
            "$label  ${
                String.format(
                    Locale.US,
                    "%.2f",
                    initial
                )
            }"

        parent.addView(labelView)

        parent.addView(seekBar)
    }

    private fun addSwitch(
        parent: LinearLayout,
        label: String,
        initial: Boolean,
        change: (Boolean) -> Unit
    ) {

        val switch =
            SwitchCompat(this).apply {

                text = label

                setTextC
