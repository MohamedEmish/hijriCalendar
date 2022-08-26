package com.amosh.hijricalendar

import android.content.Context
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton

/**
 * Fake Button class, used so TextViews can announce themselves as Buttons, for accessibility.
 */
class AccessibleLinearLayout(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = MaterialButton::class.java.name
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = MaterialButton::class.java.name
    }
}