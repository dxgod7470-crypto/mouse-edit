package com.sazanx.mouseconfigurator.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MouseAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Receive supported accessibility events from other apps.
    }

    override fun onInterrupt() {
        // Accessibility service interrupted.
    }
}
