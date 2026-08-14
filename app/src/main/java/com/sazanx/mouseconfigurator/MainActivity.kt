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

        try {
            buildUi()
        } catch (e: Throwable) {
            e.printStackTrace()

            Toast.makeText(
                this,
                "Startup error: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            updateShizukuStatus()
        } catch (e: Throwable) {
            e.printStackTrace()
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

        root.addView(
            TextView(this).apply {
                text = "Mouse Configurator v5"
                textSize = 25f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
        )

        status = TextView(this).apply {
            text = try {
                shizuku.statusText()
            } catch (e: Throwable) {
                "Shizuku status unavailable"
            }

            textSize = 13f
            setTextColor(Color.LTGRAY)
        }

        root.addView(status)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }

        controls.addView(
            Button(this).apply {
                text = "START"

                setOnClickListener {
                    startMouseService()
                }
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        controls.addView(
            Button(this).apply {
                text = "STOP"

                setOnClickListener {
                    stopMouseService()
                }
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        root.addView(controls)

        val tabs = LinearLayout(this)

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

        buttons.indices.forEach { i ->

            tabs.addView(
                Button(this).apply {
                    text = buttons[i]
                },
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

                e.printStackTrace()

                addHeader(
                    body,
                    "Page error"
                )

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

            val rg = RadioGroup(this)

            listOf(
                "Linear",
                "Soft",
                "Windows-like",
                "Aggressive"
            ).forEach { name ->

                rg.addView(
                    RadioButton(this).apply {

                        text = name

                        setTextColor(
                            Color.WHITE
                        )

                        isChecked =
                            name == cfg.curve

                        setOnClickListener {

                            cfg = cfg.copy(
                                curve = name
                            )

                            pushConfig()
                        }
                    }
                )
            }

            body.addView(rg)

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

                text =
                    "Waiting for mouse/keyboard…"

                setTextColor(
                    Color.rgb(
                        180,
                        220,
                        255
                    )
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

                    val dev =
                        InputDevice.getDevice(id)

                    if (
                        dev != null &&
                        (
                            dev.sources and
                                (
                                    InputDevice.SOURCE_MOUSE or
                                        InputDevice.SOURCE_KEYBOARD
                                )
                        ) != 0
                    ) {

                        addText(
                            body,
                            "${dev.name} " +
                                "(ID ${dev.id}) • " +
                                "sources 0x${
                                    Integer.toHexString(
                                        dev.sources
                                    )
                                }"
                        )
                    }
                }

            } catch (e: Throwable) {

                e.printStackTrace()

                addText(
                    body,
                    "Unable to read input devices."
                )
            }

            addHeader(
                body,
                "Shizuku"
            )

            addText(
                body,
                try {
                    shizuku.statusText()
                } catch (e: Throwable) {
                    "Shizuku unavailable"
                }
            )

            try {

                if (
                    shizuku.isRunning() &&
                    !shizuku.hasPermission()
                ) {

                    body.addView(
                        Button(this).apply {

                            text =
                                "Request Shizuku permission"

                            setOnClickListener {

                                try {

                                    shizuku.requestPermission()

                                    updateShizukuStatus()

                                } catch (e: Throwable) {

                                    e.printStackTrace()

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

                e.printStackTrace()
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
                    if (
                        manager?.isActive() == true
                    ) {
                        "active"
                    } else {
                        "stopped"
                    }
                }"
            )

            try {

                addText(
                    body,
                    "Thermal: ${
                        thermalMonitor.status()
                    }"
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
                    "Battery: ${
                        batteryMonitor.percent()
                    }%"
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
                    "App memory: " +
                        "${mem.appMemoryMb} MB • " +
                        "Available RAM: " +
                        "${mem.availableMemoryMb} MB / " +
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
                        inputStats.snapshot()
                            .totalEvents
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

        val pages = listOf(
            { configPage() },
            { testPage() },
            { devicePage() },
            { optimizationPage() }
        )

        buttons.indices.forEach { i ->

            tabs.getChildAt(i)
                .setOnClickListener {
                    pages[i].invoke()
                }
        }

        configPage()
    }

    private fun pushConfig() {

        try {

            MouseStabilizerService.currentConfig =
                cfg

        } catch (e: Throwable) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Unable to apply configuration",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun addHeader(
        p: LinearLayout,
        s: String
    ) {

        p.addView(
            TextView(this).apply {

                text = s

                textSize = 19f

                typeface =
                    Typeface.DEFAULT_BOLD

                setTextColor(
                    Color.WHITE
                )

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
        p: LinearLayout,
        s: String
    ) {

        p.addView(
            TextView(this).apply {

                text = s

                setTextColor(
                    Color.LTGRAY
                )

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
        p: LinearLayout,
        label: String,
        minVal: Float,
        maxVal: Float,
        initial: Float,
        change: (Float) -> Unit
    ) {

        val tv =
            TextView(this).apply {

                setTextColor(
                    Color.WHITE
                )

                textSize = 14f
            }

        val sb =
            SeekBar(this).apply {
                max = 1000
            }

        fun updateProgress(
            progressValue: Int
        ) {

            val calculatedValue =
                minVal +
                    (maxVal - minVal) *
                    (progressValue / 1000f)

            tv.text =
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
                (initial - minVal) /
                    (maxVal - minVal) *
                    1000f
                )
                .toInt()
                .coerceIn(
                    0,
                    1000
                )

        sb.progress =
            initialProgress

        sb.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBarWidget: SeekBar?,
                    progressVal: Int,
                    fromUser: Boolean
                ) {

                    if (fromUser) {
                        updateProgress(
                            progressVal
                        )
                    }
                }

                override fun onStartTrackingTouch(
                    seekBarWidget: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBarWidget: SeekBar?
                ) {
                }
            }
        )

        tv.text =
            "$label  ${
                String.format(
                    Locale.US,
                    "%.2f",
                    initial
                )
            }"

        p.addView(tv)
        p.addView(sb)
    }

    private fun addSwitch(
        p: LinearLayout,
        label: String,
        initial: Boolean,
        change: (Boolean) -> Unit
    ) {

        p.addView(
            SwitchCompat(this).apply {

                text = label

                setTextColor(
                    Color.WHITE
                )

                isChecked = initial

                setOnCheckedChangeListener {
                        _,
                        value ->
                    change(value)
                }
            }
        )
    }

    private fun startMouseService() {

        try {

            if (!shizuku.isRunning()) {

                Toast.makeText(
                    this,
                    "Start Shizuku first.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            if (!shizuku.hasPermission()) {

                shizuku.requestPermission()

                Toast.makeText(
                    this,
                    "Grant Shizuku permission, then press START again.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val intent =
                Intent(
                    this,
                    MouseStabilizerService::class.java
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                startForegroundService(intent)

            } else {

                startService(intent)
            }

            Toast.makeText(
                this,
                "Input pipeline started.",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Throwable) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Failed to start service: ${
                    e.localizedMessage
                }",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopMouseService() {

        try {

            stopService(
                Intent(
                    this,
                    MouseStabilizerService::class.java
                )
            )

            Toast.makeText(
                this,
                "Service stopped.",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Throwable) {

            e.printStackTrace()
        }
    }

    override fun dispatchGenericMotionEvent(
        e: MotionEvent
    ): Boolean {

        if (
            (
                e.source and
                    InputDevice.SOURCE_MOUSE
            ) ==
            InputDevice.SOURCE_MOUSE
        ) {

            val now =
                System.nanoTime()

            val snap =
                inputStats.record(now)

            val rawX =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O
                ) {

                    e.getAxisValue(
                        MotionEvent.AXIS_RELATIVE_X
                    )

                } else {

                    e.getAxisValue(
                        MotionEvent.AXIS_X
                    )
                }

            val rawY =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O
                ) {

                    e.getAxisValue(
                        MotionEvent.AXIS_RELATIVE_Y
                    )

                } else {

                    e.getAxisValue(
                        MotionEvent.AXIS_Y
                    )
                }

            val wheel =
                e.getAxisValue(
                    MotionEvent.AXIS_VSCROLL
                )

            val dt =
                if (lastNs == 0L) {
                    0.0
                } else {
                    (
                        now - lastNs
                    ) / 1_000_000.0
                }

            lastNs = now

            try {

                MouseStabilizerService
                    .instance
                    ?.processAndInjectInput(
                        rawX,
                        rawY
                    )

            } catch (e: Throwable) {

                e.printStackTrace()
            }

            if (::log.isInitialized) {

                val hz =
                    if (snap.hz > 0) {

                        String.format(
                            Locale.US,
                            "%.0f Hz",
                            snap.hz
                        )

                    } else {

                        "measuring"
                    }

                log.text =
                    "Raw dx=${
                        String.format(
                            Locale.US,
                            "%.2f",
                            rawX
                        )
                    } " +
                    "dy=${
                        String.format(
                            Locale.US,
                            "%.2f",
                            rawY
                        )
                    } " +
                    "wheel=${
                        String.format(
                            Locale.US,
                            "%.2f",
                            wheel
                        )
                    }\n" +
                    "Δt=${
                        String.format(
                            Locale.US,
                            "%.2f",
                            dt
                        )
                    } ms • $hz\n\n" +
                    log.text.take(2500)
            }
        }

        return super.dispatchGenericMotionEvent(e)
    }

    override fun dispatchKeyEvent(
        e: KeyEvent
    ): Boolean {

        if (
            e.action ==
            KeyEvent.ACTION_DOWN &&
            ::log.isInitialized
        ) {

            val d =
                InputDevice.getDevice(
                    e.deviceId
                )

            log.text =
                "KEY ${
                    KeyEvent.keyCodeToString(
                        e.keyCode
                    )
                } • ${
                    d?.name ?: "Unknown"
                }\n\n" +
                log.text.take(2500)
        }

        return super.dispatchKeyEvent(e)
    }

    private fun startStatsTimer() {

        stopStatsTimer()

        val r =
            object : Runnable {

                override fun run() {

                    if (
                        ::status.isInitialized
                    ) {
                        updateShizukuStatus()
                    }

                    if (
                        statsTimer === this
                    ) {

                        mainHandler.postDelayed(
                            this,
                            3000L
                        )
                    }
                }
            }

        statsTimer = r

        mainHandler.post(r)
    }

    private fun stopStatsTimer() {

        statsTimer?.let {
            mainHandler.removeCallbacks(it)
        }

        statsTimer = null
    }
}
