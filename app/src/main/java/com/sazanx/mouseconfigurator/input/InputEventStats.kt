package com.sazanx.mouseconfigurator.input

class InputEventStats {
    private var eventCount = 0L
    private var lastTimeNs = System.nanoTime()
    private var currentHz = 0.0

    data class Snapshot(val totalEvents: Long, val hz: Double)

    @Synchronized
    fun record(nowNs: Long): Snapshot {
        eventCount++
        val elapsed = (nowNs - lastTimeNs) / 1_000_000_000.0
        if (elapsed >= 1.0) {
            currentHz = eventCount / elapsed
            eventCount = 0
            lastTimeNs = nowNs
        }
        return Snapshot(eventCount, currentHz)
    }

    @Synchronized
    fun snapshot(): Snapshot = Snapshot(eventCount, currentHz)
}
