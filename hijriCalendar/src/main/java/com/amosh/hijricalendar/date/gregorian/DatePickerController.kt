package com.amosh.hijricalendar.date.gregorian

import java.util.*

/**
 * Controller class to communicate among the various components of the date picker dialog.
 */
interface DatePickerController {
    fun onYearSelected(year: Int)
    fun onDayOfMonthSelected(year: Int, month: Int, day: Int)
    fun registerOnDateChangedListener(listener: GregorianDatePickerDialog.OnDateChangedListener)
    fun unregisterOnDateChangedListener(listener: GregorianDatePickerDialog.OnDateChangedListener)
    val selectedDay: MonthAdapter.CalendarDay
    val isThemeDark: Boolean
    val accentColor: Int
    fun isHighlighted(year: Int, month: Int, day: Int): Boolean
    val firstDayOfWeek: Int
    val minYear: Int
    val maxYear: Int
    val startDate: Calendar
    val endDate: Calendar
    fun isOutOfRange(year: Int, month: Int, day: Int): Boolean
    fun tryVibrate()
    val timeZone: TimeZone
    val locale: Locale
}