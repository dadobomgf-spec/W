package com.aiassistant.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AccessibilityHelper : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Placeholder for future features
    }

    override fun onInterrupt() {
        // Required
    }

    companion object {
        var instance: AccessibilityHelper? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
