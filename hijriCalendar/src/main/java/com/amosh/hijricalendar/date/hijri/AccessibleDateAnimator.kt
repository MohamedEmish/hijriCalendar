package com.amosh.hijricalendar.date.hijri

import android.content.Context
import android.util.AttributeSet
import android.view.accessibility.AccessibilityEvent
import android.widget.ViewAnimator

class AccessibleDateAnimator(context: Context?, attrs: AttributeSet?) : ViewAnimator(context, attrs) {
    private var mDateMillis: Long = 0
    fun setDateMillis(dateMillis: Long) {
        mDateMillis = dateMillis
    }

    /**
     * Announce the currently-selected date when launched.
     */
    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Clear the event's current text so that only the current date will be spoken.
            event.text.clear()
            val dateString = "HH"
            event.text.add(dateString)
            return true
        }
        return super.dispatchPopulateAccessibilityEvent(event)
    }
}