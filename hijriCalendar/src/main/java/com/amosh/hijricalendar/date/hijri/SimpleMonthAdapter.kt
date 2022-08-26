package com.amosh.hijricalendar.date.hijri

import android.content.Context

/**
 * An adapter for a list of [SimpleMonthView] items.
 */
class SimpleMonthAdapter(context: Context, controller: DatePickerController) : MonthAdapter(context, controller) {
    override fun createMonthView(context: Context): MonthView {
        return SimpleMonthView(context, null, mController)
    }
}