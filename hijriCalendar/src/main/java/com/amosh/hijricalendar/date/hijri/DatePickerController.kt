/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amosh.hijricalendar.date.hijri

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import java.util.*

/**
 * Controller class to communicate among the various components of the date picker dialog.
 */
interface DatePickerController {
    fun onYearSelected(year: Int)
    fun onDayOfMonthSelected(year: Int, month: Int, day: Int)
    fun registerOnDateChangedListener(listener: HijriDatePickerDialog.OnDateChangedListener)
    fun unregisterOnDateChangedListener(listener: HijriDatePickerDialog.OnDateChangedListener)
    val selectedDay: MonthAdapter.CalendarDay
    val isThemeDark: Boolean
    val accentColor: Int
    fun isHighlighted(year: Int, month: Int, day: Int): Boolean
    val firstDayOfWeek: Int
    val minYear: Int
    val maxYear: Int
    val startDate: UmmalquraCalendar
    val endDate: UmmalquraCalendar
    fun isOutOfRange(year: Int, month: Int, day: Int): Boolean
    fun tryVibrate()
    val timeZone: TimeZone
    val locale: Locale
}