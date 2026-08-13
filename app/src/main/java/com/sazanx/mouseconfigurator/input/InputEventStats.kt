package com.sazanx.mouseconfigurator.input

class InputEventStats {
    private var windowStartNs = 0L
    private var windowEvents = 0
    private var totalEvents = 0L
    private var lastEventNs = 0L

    @Synchronized
    fun record(nowNs: Long = System.nanoTime()): Snapshot {
        if (windowStartNs == 0L) windowStartNs = nowNs
        windowEvents++
        totalEvents++
        val intervalMs = if (lastEventNs == 0L) 0.0 else (nowNs - lastEventNs) / 1_000_000.0
        lastEventNs = nowNs

        val elapsed = nowNs - windowStartNs
        val hz = if (elapsed >= 1_000_000_000L) {
            val value = windowEvents * 1_000_000_000.0 / elapsed
            windowEvents = 0
            windowStartNs = nowNs
            value
        } else 0.0
        return Snapshot(totalEvents, intervalMs, hz)
    }

    @Synchronized
    fun snapshot(): Snapshot = Snapshot(totalEvents, 0.0, 0.0)

    @Synchronized
    fun reset() {
        windowStartNs = 0L
        windowEvents = 0
        totalEvents = 0L
        lastEventNs = 0L
    }

    data class Snapshot(val totalEvents: Long, val intervalMs: Double, val hz: Double)
}
