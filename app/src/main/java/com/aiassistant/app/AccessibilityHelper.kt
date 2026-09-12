package com.aiassistant.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class AccessibilityHelper : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // لا نحتاج معالجة الأحداث حالياً
    }

    override fun onInterrupt() {
        // مطلوب
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // النقر على إحداثيات محددة (x, y)
    fun clickAt(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path()
        path.moveTo(x, y)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    // التمرير من نقطة إلى نقطة
    fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path()
        path.moveTo(fromX, fromY)
        path.lineTo(toX, toY)

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 400))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    // زر الرجوع
    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    // زر الرئيسية
    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    companion object {
        var instance: AccessibilityHelper? = null
    }
}
