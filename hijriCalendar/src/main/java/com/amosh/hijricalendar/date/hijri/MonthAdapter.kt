package com.amosh.hijricalendar.date.hijri

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import java.util.*

/**
 * An adapter for a list of [MonthView] items.
 */
abstract class MonthAdapter(
    private val mContext: Context,
    val mController: DatePickerController,
) : BaseAdapter(), MonthView.OnDayClickListener {
    private var mSelectedDay: CalendarDay? = null

    /**
     * A convenience class to represent a specific date.
     */
    class CalendarDay {
        private var calendar: UmmalquraCalendar? = null
        var year = 0
        var month = 0
        var day = 0
        private var mTimeZone: TimeZone? = null

        constructor(timeZone: TimeZone?) {
            mTimeZone = timeZone
            setTime(System.currentTimeMillis())
        }

        constructor(timeInMillis: Long, timeZone: TimeZone?) {
            mTimeZone = timeZone
            setTime(timeInMillis)
        }

        constructor(calendar: UmmalquraCalendar, timeZone: TimeZone?) {
            mTimeZone = timeZone
            year = calendar.get(UmmalquraCalendar.YEAR)
            month = calendar.get(UmmalquraCalendar.MONTH)
            day = calendar.get(UmmalquraCalendar.DAY_OF_MONTH)
        }

        constructor(year: Int, month: Int, day: Int) {
            setDay(year, month, day)
        }

        fun set(date: CalendarDay?) {
            year = date!!.year
            month = date.month
            day = date.day
        }

        private fun setDay(year: Int, month: Int, day: Int) {
            this.year = year
            this.month = month
            this.day = day
        }

        private fun setTime(timeInMillis: Long) {
            if (calendar == null) {
                calendar = UmmalquraCalendar(mTimeZone, Locale.getDefault())
            }
            calendar!!.timeInMillis = timeInMillis
            month = calendar!!.get(UmmalquraCalendar.MONTH)
            year = calendar!!.get(UmmalquraCalendar.YEAR)
            day = calendar!!.get(UmmalquraCalendar.DAY_OF_MONTH)
        }
    }

    /**
     * Updates the selected day and related parameters.
     *
     * @param day The day to highlight
     */
    var selectedDay: CalendarDay?
        get() = mSelectedDay
        set(day) {
            mSelectedDay = day
            notifyDataSetChanged()
        }

    /**
     * Set up the gesture detector and selected time
     */
    fun init() {
        mSelectedDay = CalendarDay(System.currentTimeMillis(), mController.timeZone)
    }

    override fun getCount(): Int {
        val endDate: UmmalquraCalendar = mController.endDate
        val startDate: UmmalquraCalendar = mController.startDate
        val endMonth: Int = endDate.get(UmmalquraCalendar.YEAR) * MONTHS_IN_YEAR + endDate.get(UmmalquraCalendar.MONTH)
        val startMonth: Int = startDate.get(UmmalquraCalendar.YEAR) * MONTHS_IN_YEAR + startDate.get(UmmalquraCalendar.MONTH)
        return endMonth - startMonth + 1
        //return ((mController.getMaxYear() - mController.getMinYear()) + 1) * MONTHS_IN_YEAR;
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    @SuppressLint("NewApi")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v: MonthView
        var drawingParams: HashMap<String?, Int?>? = null
        if (convertView != null) {
            v = convertView as MonthView
            // We store the drawing parameters in the view so it can be recycled
            drawingParams = v.tag as HashMap<String?, Int?>
        } else {
            v = createMonthView(mContext)
            // Set up the new view
            val params: AbsListView.LayoutParams = AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT)
            v.layoutParams = params
            v.isClickable = true
            v.setOnDayClickListener(this)
        }
        if (drawingParams == null) {
            drawingParams = HashMap()
        }
        drawingParams.clear()
        val month: Int = (position + mController.startDate.get(UmmalquraCalendar.MONTH)) % MONTHS_IN_YEAR
        val year: Int = (position + mController.startDate.get(UmmalquraCalendar.MONTH)) / MONTHS_IN_YEAR + mController.minYear
        var selectedDay = -1
        if (isSelectedDayInMonth(year, month)) {
            selectedDay = mSelectedDay!!.day
        }

        // Invokes requestLayout() to ensure that the recycled view is set with the appropriate
        // height/number of weeks before being displayed.
        v.reuse()
        drawingParams[MonthView.VIEW_PARAMS_SELECTED_DAY] = selectedDay
        drawingParams[MonthView.VIEW_PARAMS_YEAR] = year
        drawingParams[MonthView.VIEW_PARAMS_MONTH] = month
        drawingParams[MonthView.VIEW_PARAMS_WEEK_START] = mController.firstDayOfWeek
        v.setMonthParams(drawingParams)
        v.invalidate()
        return v
    }

    abstract fun createMonthView(context: Context): MonthView
    private fun isSelectedDayInMonth(year: Int, month: Int): Boolean {
        return mSelectedDay!!.year == year && mSelectedDay!!.month == month
    }

    override fun onDayClick(view: MonthView?, day: CalendarDay?) {
        day?.let { onDayTapped(it) }
    }

    /**
     * Maintains the same hour/min/sec but moves the day to the tapped day.
     *
     * @param day The day that was tapped
     */
    private fun onDayTapped(day: CalendarDay) {
        mController.tryVibrate()
        mController.onDayOfMonthSelected(day.year, day.month, day.day)
        selectedDay = day
    }

    companion object {
        const val MONTHS_IN_YEAR = 12
    }

    init {
        init()
        selectedDay = mController.selectedDay
    }
}