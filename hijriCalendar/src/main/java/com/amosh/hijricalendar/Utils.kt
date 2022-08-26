package com.amosh.hijricalendar

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes


/**
 * Utility helper functions for time and date pickers.
 */
object Utils {
    //public static final int MONDAY_BEFORE_JULIAN_EPOCH = Time.EPOCH_JULIAN_DAY - 3;
    private const val PULSE_ANIMATOR_DURATION = 544

    /**
     * Try to speak the specified text, for accessibility. Only available on JB or later.
     * @param text Text to announce.
     */
    @SuppressLint("NewApi")
    fun tryAccessibilityAnnounce(view: View?, text: CharSequence?) {
        if (view != null && text != null) {
            view.announceForAccessibility(text)
        }
    }
    /**
     * public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
     * int diff = Time.THURSDAY - firstDayOfWeek;
     * if (diff < 0) {
     * diff += 7;
     * }
     * int refDay = Time.EPOCH_JULIAN_DAY - diff;
     * return (julianDay - refDay) / 7;
     * }
     */
    /**
     * Render an animator to pulsate a view in place.
     * @param labelToAnimate the view to pulsate.
     * @return The animator object. Use .start() to begin.
     */
    fun getPulseAnimator(
        labelToAnimate: View?, decreaseRatio: Float,
        increaseRatio: Float,
    ): ObjectAnimator {
        val k0 = Keyframe.ofFloat(0f, 1f)
        val k1 = Keyframe.ofFloat(0.275f, decreaseRatio)
        val k2 = Keyframe.ofFloat(0.69f, increaseRatio)
        val k3 = Keyframe.ofFloat(1f, 1f)
        val scaleX = PropertyValuesHolder.ofKeyframe("scaleX", k0, k1, k2, k3)
        val scaleY = PropertyValuesHolder.ofKeyframe("scaleY", k0, k1, k2, k3)
        val pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(labelToAnimate, scaleX, scaleY)
        pulseAnimator.duration = PULSE_ANIMATOR_DURATION.toLong()
        return pulseAnimator
    }

    fun darkenColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = hsv[2] * 0.8f // value component
        return Color.HSVToColor(hsv)
    }

    /**
     * Gets the colorAccent from the current context, if possible/available
     * @param context The context to use as reference for the color
     * @return the accent color of the current context
     */
    fun getAccentColorFromThemeIfAvailable(context: Context): Int {
        val typedValue = TypedValue()
        // First, try the android:colorAccent
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    /**
     * Gets dialog type (Light/Dark) from current theme
     * @param context The context to use as reference for the boolean
     * @param current Default value to return if cannot resolve the attribute
     * @return true if dark mode, false if light.
     */
    fun isDarkTheme(context: Context, current: Boolean): Boolean {
        return resolveBoolean(context, R.attr.mdtp_theme_dark, current)
    }

    /**
     * Gets the required boolean value from the current context, if possible/available
     * @param context The context to use as reference for the boolean
     * @param attr Attribute id to resolve
     * @param fallback Default value to return if no value is specified in theme
     * @return the boolean value from current theme
     */
    private fun resolveBoolean(context: Context, @AttrRes attr: Int, fallback: Boolean): Boolean {
        val a = context.theme.obtainStyledAttributes(intArrayOf(attr))
        return try {
            a.getBoolean(0, fallback)
        } finally {
            a.recycle()
        }
    }
}
