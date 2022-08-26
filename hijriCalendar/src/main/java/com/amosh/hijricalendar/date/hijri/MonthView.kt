package com.amosh.hijricalendar.date.hijri

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.amosh.hijricalendar.R
import com.amosh.hijricalendar.TypefaceHelper
import com.amosh.hijricalendar.date.gregorian.MonthView.Companion.monthHeaderSize
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.util.*

/**
 * A calendar-like view displaying a specified month and the appropriate selectable day numbers
 * within the specified month.
 */
abstract class MonthView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    var mController: DatePickerController,
) : View(context, attr) {
    // affects the padding on the sides of this view
    private val DATE_FORMAT = "dd MMMM yyyy"

    var mEdgePadding = 0
    private val mDayOfWeekTypeface: String
    private val mMonthTitleTypeface: String
    var mMonthNumPaint: Paint? = null
    private var mMonthTitlePaint: Paint? = null
    var mSelectedCirclePaint: Paint? = null
    private var mMonthDayLabelPaint: Paint? = null
    private val mFormatter: Formatter
    private val mStringBuilder: StringBuilder

    var month = 0
    var year = 0

    // Quick reference to the width of this view, matches parent
    var mWidth = 0

    // The height this view should draw at in pixels, set by height param
    var mRowHeight = DEFAULT_HEIGHT

    // If this view contains the today
    var mHasToday = false

    // Which day is selected [0-6] or -1 if no day is selected
    var mSelectedDay = -1

    // Which day is today [0-6] or -1 if no day is today
    var mToday = DEFAULT_SELECTED_DAY

    // Which day of the week to start on [0-6]
    private var mWeekStart = DEFAULT_WEEK_START

    // How many days to display
    var mNumDays = DEFAULT_NUM_DAYS

    // The number of days + a spot for week number if it is displayed
    var mNumCells = mNumDays

    private val mCalendar: UmmalquraCalendar
    private val mDayLabelCalendar: UmmalquraCalendar
    private val mTouchHelper: MonthViewTouchHelper
    private var mNumRows = DEFAULT_NUM_ROWS

    // Optional listener for handling day click actions
    private var mOnDayClickListener: OnDayClickListener? = null

    // Whether to prevent setting the accessibility delegate
    private val mLockAccessibilityDelegate: Boolean
    var mDayTextColor = 0
    var mSelectedDayTextColor: Int
    private var mMonthDayTextColor = 0
    var mTodayNumberColor: Int
    var mHighlightedDayTextColor = 0
    var mDisabledDayTextColor = 0
    private var mMonthTitleColor: Int

    private val monthViewTouchHelper: MonthViewTouchHelper
        get() = MonthViewTouchHelper(this)

    override fun setAccessibilityDelegate(delegate: AccessibilityDelegate?) {
        // Workaround for a JB MR1 issue where accessibility delegates on
        // top-level ListView items are overwritten.
        if (!mLockAccessibilityDelegate) {
            super.setAccessibilityDelegate(delegate)
        }
    }

    fun setOnDayClickListener(listener: OnDayClickListener?) {
        mOnDayClickListener = listener
    }

    public override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        // First right-of-refusal goes the touch exploration helper.
        return if (mTouchHelper.dispatchHoverEvent(event)) {
            true
        } else super.dispatchHoverEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val day = getDayFromLocation(event.x, event.y)
                if (day >= 0) {
                    onDayClick(day)
                }
            }
        }
        return true
    }

    /**
     * Sets up the text and style properties for painting. Override this if you
     * want to use a different paint.
     */
    private fun initView() {
        mMonthTitlePaint = Paint()
        mMonthTitlePaint!!.isFakeBoldText = true
        mMonthTitlePaint!!.isAntiAlias = true
        mMonthTitlePaint!!.textSize = MONTH_LABEL_TEXT_SIZE.toFloat()
        mMonthTitlePaint!!.typeface = Typeface.create(mMonthTitleTypeface, Typeface.BOLD)
        mMonthTitlePaint!!.color = mDayTextColor
        mMonthTitlePaint!!.textAlign = Paint.Align.CENTER
        mMonthTitlePaint!!.style = Paint.Style.FILL
        mMonthTitlePaint!!.textLocale = mController.locale
        mSelectedCirclePaint = Paint()
        mSelectedCirclePaint!!.isFakeBoldText = true
        mSelectedCirclePaint!!.isAntiAlias = true
        mSelectedCirclePaint!!.color = mTodayNumberColor
        mSelectedCirclePaint!!.textAlign = Paint.Align.CENTER
        mSelectedCirclePaint!!.style = Paint.Style.FILL
        mSelectedCirclePaint!!.alpha = SELECTED_CIRCLE_ALPHA
        mMonthDayLabelPaint = Paint()
        mMonthDayLabelPaint!!.isAntiAlias = true
        mMonthDayLabelPaint!!.textSize = MONTH_DAY_LABEL_TEXT_SIZE.toFloat()
        mMonthDayLabelPaint!!.color = mMonthDayTextColor
        mMonthDayLabelPaint!!.typeface = TypefaceHelper[context, "Roboto-Medium"]
        mMonthDayLabelPaint!!.style = Paint.Style.FILL
        mMonthDayLabelPaint!!.textAlign = Paint.Align.CENTER
        mMonthDayLabelPaint!!.isFakeBoldText = true
        mMonthDayLabelPaint!!.textLocale = mController.locale
        mMonthNumPaint = Paint()
        mMonthNumPaint!!.isAntiAlias = true
        mMonthNumPaint!!.textSize = MINI_DAY_NUMBER_TEXT_SIZE.toFloat()
        mMonthNumPaint!!.style = Paint.Style.FILL
        mMonthNumPaint!!.textAlign = Paint.Align.CENTER
        mMonthNumPaint!!.isFakeBoldText = false
        mMonthNumPaint!!.textLocale = mController.locale
    }

    override fun onDraw(canvas: Canvas) {
        drawMonthTitle(canvas)
        drawMonthDayLabels(canvas)
        drawMonthNums(canvas)
    }

    private var mDayOfWeekStart = 0

    /**
     * Sets all the parameters for displaying this week. The only required
     * parameter is the week number. Other parameters have a default value and
     * will only update if a new value is included, except for focus month,
     * which will always default to no focus month if no value is passed in. See
     * [.VIEW_PARAMS_HEIGHT] for more info on parameters.
     *
     * @param params A map of the new parameters, see
     * [.VIEW_PARAMS_HEIGHT]
     */
    fun setMonthParams(params: HashMap<String?, Int?>) {
        if (!params.containsKey(VIEW_PARAMS_MONTH) && !params.containsKey(VIEW_PARAMS_YEAR)) {
            throw InvalidParameterException("You must specify month and year for this view")
        }
        tag = params
        // We keep the current value for any params not present
        if (params.containsKey(VIEW_PARAMS_HEIGHT)) {
            mRowHeight = params[VIEW_PARAMS_HEIGHT]!!
            if (mRowHeight < MIN_HEIGHT) {
                mRowHeight = MIN_HEIGHT
            }
        }
        if (params.containsKey(VIEW_PARAMS_SELECTED_DAY)) {
            mSelectedDay = params[VIEW_PARAMS_SELECTED_DAY]!!
        }

        // Allocate space for caching the day numbers and focus values
        month = params[VIEW_PARAMS_MONTH]!!
        year = params[VIEW_PARAMS_YEAR]!!

        // Figure out what day today is
        //final Time today = new Time(Time.getCurrentTimezone());
        //today.setToNow();
        val today = UmmalquraCalendar(mController.timeZone, mController.locale)
        mHasToday = false
        mToday = -1
        mCalendar.set(UmmalquraCalendar.MONTH, month)
        mCalendar.set(UmmalquraCalendar.YEAR, year)
        mCalendar.set(UmmalquraCalendar.DAY_OF_MONTH, 1)
        mDayOfWeekStart = mCalendar.get(UmmalquraCalendar.DAY_OF_WEEK)
        mWeekStart = if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
            params[VIEW_PARAMS_WEEK_START]!!
        } else {
            mCalendar.firstDayOfWeek
        }
        mNumCells = UmmalquraCalendar.lengthOfMonth(mCalendar.get(UmmalquraCalendar.YEAR), mCalendar.get(UmmalquraCalendar.MONTH))

//        mNumCells = mCalendar.getActualMaximum(UmmalquraCalendar.DAY_OF_MONTH);
        for (i in 0 until mNumCells) {
            val day = i + 1
            if (sameDay(day, today)) {
                mHasToday = true
                mToday = day
            }
        }
        mNumRows = calculateNumRows()

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot()
    }

    fun setSelectedDay(day: Int) {
        mSelectedDay = day
    }

    fun reuse() {
        mNumRows = DEFAULT_NUM_ROWS
        requestLayout()
    }

    private fun calculateNumRows(): Int {
        val offset = findDayOffset()
        val dividend = (offset + mNumCells) / mNumDays
        val remainder = (offset + mNumCells) % mNumDays
        return dividend + if (remainder > 0) 1 else 0
    }

    private fun sameDay(day: Int, today: UmmalquraCalendar): Boolean {
        return year == today.get(UmmalquraCalendar.YEAR) && month == today.get(UmmalquraCalendar.MONTH) && day == today.get(UmmalquraCalendar.DAY_OF_MONTH)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mRowHeight * mNumRows + monthHeaderSize + 5)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mWidth = w

        // Invalidate cached accessibility information.
        mTouchHelper.invalidateRoot()
    }

    private val monthAndYearString: String
        get() {
            val locale = mController.locale
            var pattern: String? = "MMMM yyyy"
            pattern =
                DateFormat.getBestDateTimePattern(
                    locale,
                    pattern)
            val formatter = SimpleDateFormat(pattern, locale)
            formatter.timeZone = mController.timeZone
            formatter.applyLocalizedPattern(pattern)
            mStringBuilder.setLength(0)
            return (mCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)?.toString() ?: "") + " " + mCalendar.get(Calendar.YEAR)
        }

    private fun drawMonthTitle(canvas: Canvas) {
        val x = (mWidth + 2 * mEdgePadding) / 2
        val y = (monthHeaderSize - MONTH_DAY_LABEL_TEXT_SIZE) / 2
        canvas.drawText(monthAndYearString, x.toFloat(), y.toFloat(), mMonthTitlePaint!!)
    }

    private fun drawMonthDayLabels(canvas: Canvas) {
        val y = monthHeaderSize - MONTH_DAY_LABEL_TEXT_SIZE / 2
        val dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2)
        for (i in 0 until mNumDays) {
            val x = (2 * i + 1) * dayWidthHalf + mEdgePadding
            val calendarDay = (i + mWeekStart) % mNumDays
            mDayLabelCalendar.set(UmmalquraCalendar.DAY_OF_WEEK, calendarDay)
            val weekString = getWeekDayLabel(mDayLabelCalendar)
            canvas.drawText(weekString, x.toFloat(), y.toFloat(), mMonthDayLabelPaint!!)
        }
    }

    /**
     * Draws the week and month day numbers for this week. Override this method
     * if you need different placement.
     *
     * @param canvas The canvas to draw on
     */
    private fun drawMonthNums(canvas: Canvas) {
        var y = ((mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2 - DAY_SEPARATOR_WIDTH
            + monthHeaderSize)
        val dayWidthHalf = (mWidth - mEdgePadding * 2) / (mNumDays * 2.0f)
        var j = findDayOffset()
        for (dayNumber in 1..mNumCells) {
            val x = ((2 * j + 1) * dayWidthHalf + mEdgePadding).toInt()
            val yRelativeToDay = (mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2 - DAY_SEPARATOR_WIDTH
            val startX = (x - dayWidthHalf).toInt()
            val stopX = (x + dayWidthHalf).toInt()
            val startY = (y - yRelativeToDay)
            drawMonthDay(canvas, year, month, dayNumber, x, y, startX, stopX, startY, (startY + mRowHeight))
            j++
            if (j == mNumDays) {
                j = 0
                y += mRowHeight
            }
        }
    }

    /**
     * This method should draw the month day.  Implemented by sub-classes to allow customization.
     *
     * @param canvas The canvas to draw on
     * @param year   The year of this month day
     * @param month  The month of this month day
     * @param day    The day number of this month day
     * @param x      The default x position to draw the day number
     * @param y      The default y position to draw the day number
     * @param startX The left boundary of the day number rect
     * @param stopX  The right boundary of the day number rect
     * @param startY The top boundary of the day number rect
     * @param stopY  The bottom boundary of the day number rect
     */
    abstract fun drawMonthDay(
        canvas: Canvas, year: Int, month: Int, day: Int,
        x: Int, y: Int, startX: Int, stopX: Int, startY: Int, stopY: Int,
    )

    fun findDayOffset(): Int {
        return ((if (mDayOfWeekStart < mWeekStart) mDayOfWeekStart + mNumDays else mDayOfWeekStart)
            - mWeekStart)
    }

    /**
     * Calculates the day that the given x position is in, accounting for week
     * number. Returns the day or -1 if the position wasn't in a day.
     *
     * @param x The x position of the touch event
     * @return The day number, or -1 if the position wasn't in a day
     */
    fun getDayFromLocation(x: Float, y: Float): Int {
        val day = getInternalDayFromLocation(x, y)
        return if (day < 1 || day > mNumCells) {
            -1
        } else day
    }

    /**
     * Calculates the day that the given x position is in, accounting for week
     * number.
     *
     * @param x The x position of the touch event
     * @return The day number
     */
    private fun getInternalDayFromLocation(x: Float, y: Float): Int {
        val dayStart = mEdgePadding
        if (x < dayStart || x > mWidth - mEdgePadding) {
            return -1
        }
        // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
        val row = (y - monthHeaderSize).toInt() / mRowHeight
        val column = ((x - dayStart) * mNumDays / (mWidth - dayStart - mEdgePadding)).toInt()
        var day = column - findDayOffset() + 1
        day += row * mNumDays
        return day
    }

    /**
     * Called when the user clicks on a day. Handles callbacks to the
     * [OnDayClickListener] if one is set.
     *
     *
     * If the day is out of the range set by minDate and/or maxDate, this is a no-op.
     *
     * @param day The day that was clicked
     */
    private fun onDayClick(day: Int) {
        // If the min / max date are set, only process the click if it's a valid selection.
        if (mController.isOutOfRange(year, month, day)) {
            return
        }
        if (mOnDayClickListener != null) {
            mOnDayClickListener!!.onDayClick(this, MonthAdapter.CalendarDay(year, month, day))
        }

        // This is a no-op if accessibility is turned off.
        mTouchHelper.sendEventForVirtualView(day, AccessibilityEvent.TYPE_VIEW_CLICKED)
    }

    /**
     * @param year
     * @param month
     * @param day
     * @return true if the given date should be highlighted
     */
    fun isHighlighted(year: Int, month: Int, day: Int): Boolean {
        return mController.isHighlighted(year, month, day)
    }

    /**
     * Return a 1 or 2 letter String for use as a weekday label
     *
     * @param day The day for which to generate a label
     * @return The weekday label
     */
    private fun getWeekDayLabel(day: UmmalquraCalendar): String {
        val locale = mController.locale

        // Localised short version of the string is not available on API < 18
        // Getting the short label is a one liner on API >= 18
        return SimpleDateFormat("EEEEE", locale).format(day.time)
    }

    /**
     * @return The date that has accessibility focus, or `null` if no date
     * has focus
     */
    val accessibilityFocus: MonthAdapter.CalendarDay?
        get() {
            val day: Int = mTouchHelper.accessibilityFocusedVirtualViewId
            return if (day >= 0) {
                MonthAdapter.CalendarDay(year, month, day)
            } else null
        }

    /**
     * Attempts to restore accessibility focus to the specified date.
     *
     * @param day The date which should receive focus
     * @return `false` if the date is not valid for this month view, or
     * `true` if the date received focus
     */
    fun restoreAccessibilityFocus(day: MonthAdapter.CalendarDay): Boolean {
        if (day.year !== year || day.month !== month || day.day > mNumCells) {
            return false
        }
        mTouchHelper.focusedVirtualView = day.day
        return true
    }

    /**
     * Provides a virtual view hierarchy for interfacing with an accessibility
     * service.
     */
    @Suppress("DEPRECATION")
    inner class MonthViewTouchHelper(host: View) : ExploreByTouchHelper(host) {
        private val mTempRect = Rect()
        private val mTempCalendar: UmmalquraCalendar = UmmalquraCalendar(mController.timeZone, mController.locale)
        fun setFocusedVirtualView(virtualViewId: Int) {
            getAccessibilityNodeProvider(this@MonthView)?.performAction(
                virtualViewId, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null)
        }

        override fun getVirtualViewAt(x: Float, y: Float): Int {
            val day = getDayFromLocation(x, y)
            return if (day >= 0) {
                day
            } else INVALID_ID
        }

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            for (day in 1..mNumCells) {
                virtualViewIds.add(day)
            }
        }

        override fun onPopulateEventForVirtualView(virtualViewId: Int, event: AccessibilityEvent) {
            event.contentDescription = getItemDescription(virtualViewId)
        }

        override fun onPopulateNodeForVirtualView(
            virtualViewId: Int,
            node: AccessibilityNodeInfoCompat,
        ) {
            getItemBounds(virtualViewId, mTempRect)
            node.contentDescription = getItemDescription(virtualViewId)
            node.setBoundsInParent(mTempRect)
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (virtualViewId == mSelectedDay) {
                node.isSelected = true
            }
        }

        override fun onPerformActionForVirtualView(
            virtualViewId: Int, action: Int,
            arguments: Bundle?,
        ): Boolean {
            when (action) {
                AccessibilityNodeInfo.ACTION_CLICK -> {
                    onDayClick(virtualViewId)
                    return true
                }
            }
            return false
        }

        /**
         * Calculates the bounding rectangle of a given time object.
         *
         * @param day  The day to calculate bounds for
         * @param rect The rectangle in which to store the bounds
         */
        private fun getItemBounds(day: Int, rect: Rect) {
            val offsetX = mEdgePadding
            val offsetY = monthHeaderSize
            val cellHeight = mRowHeight
            val cellWidth = (mWidth - 2 * mEdgePadding) / mNumDays
            val index = day - 1 + findDayOffset()
            val row = index / mNumDays
            val column = index % mNumDays
            val x = offsetX + column * cellWidth
            val y = offsetY + row * cellHeight
            rect[x, y, x + cellWidth] = y + cellHeight
        }

        /**
         * Generates a description for a given time object. Since this
         * description will be spoken, the components are ordered by descending
         * specificity as DAY MONTH YEAR.
         *
         * @param day The day to generate a description for
         * @return A description of the time object
         */
        private fun getItemDescription(day: Int): CharSequence {
            mTempCalendar.set(year, month, day)
            val date: CharSequence = DateFormat.format(DATE_FORMAT,
                mTempCalendar.timeInMillis)
            return if (day == mSelectedDay) {
                context.getString(R.string.mdtp_item_is_selected, date)
            } else date
        }

    }

    /**
     * Handles callbacks when the user clicks on a time object.
     */
    interface OnDayClickListener {
        fun onDayClick(view: MonthView?, day: MonthAdapter.CalendarDay?)
    }

    companion object {
        /**
         * These params can be passed into the view to control how it appears.
         * [.VIEW_PARAMS_WEEK] is the only required field, though the default
         * values are unlikely to fit most layouts correctly.
         */
        /**
         * This sets the height of this week in pixels
         */
        const val VIEW_PARAMS_HEIGHT = "height"

        /**
         * This specifies the position (or weeks since the epoch) of this week.
         */
        const val VIEW_PARAMS_MONTH = "month"

        /**
         * This specifies the position (or weeks since the epoch) of this week.
         */
        const val VIEW_PARAMS_YEAR = "year"

        /**
         * This sets one of the days in this view as selected [UmmalquraCalendar.SUNDAY]
         * through [UmmalquraCalendar.SATURDAY].
         */
        const val VIEW_PARAMS_SELECTED_DAY = "selected_day"

        /**
         * Which day the week should start on. [UmmalquraCalendar.SUNDAY] through
         * [UmmalquraCalendar.SATURDAY].
         */
        const val VIEW_PARAMS_WEEK_START = "week_start"

        var DEFAULT_HEIGHT = 32
        var MIN_HEIGHT = 10
        const val DEFAULT_SELECTED_DAY = -1
        val DEFAULT_WEEK_START: Int = UmmalquraCalendar.SUNDAY
        const val DEFAULT_NUM_DAYS = 7
        const val DEFAULT_NUM_ROWS = 6
        const val MAX_NUM_ROWS = 6
        private const val SELECTED_CIRCLE_ALPHA = 255
        var DAY_SEPARATOR_WIDTH = 1
        var MINI_DAY_NUMBER_TEXT_SIZE: Int = 0
        var MONTH_LABEL_TEXT_SIZE: Int = 0
        var MONTH_DAY_LABEL_TEXT_SIZE: Int = 0
        var DAY_SELECTED_CIRCLE_SIZE = 0


    }

    init {
        val res = context.resources
        mDayLabelCalendar = UmmalquraCalendar(mController.timeZone, mController.locale)
        mCalendar = UmmalquraCalendar(mController.timeZone, mController.locale)
        mDayOfWeekTypeface = res.getString(R.string.mdtp_day_of_week_label_typeface)
        mMonthTitleTypeface = res.getString(R.string.mdtp_sans_serif)
        val darkTheme = mController.isThemeDark
        if (darkTheme) {
            mDayTextColor = ContextCompat.getColor(context, R.color.mdtp_date_picker_text_normal_dark_theme)
            mMonthDayTextColor = ContextCompat.getColor(context, R.color.mdtp_date_picker_month_day_dark_theme)
            mDisabledDayTextColor = ContextCompat.getColor(context, R.color.mdtp_date_picker_text_disabled_dark_theme)
            mHighlightedDayTextColor = ContextCompat.getColor(context, R.color.mdtp_date_picker_text_highlighted_dark_theme)
        } else {
            mDayTextColor = ContextCompat.getColor(context, R.color.mdtp_date_picker_text_normal)
            mMonthDayTextColor = ContextCompat.getColor(context, R.color.mdtp_date_picker_month_day)
            mDisabledDayTextColor = ContextCompat.getColor(context, R.color.mdtp_date_picker_text_disabled)
            mHighlightedDayTextColor = ContextCompat.getColor(context, R.color.mdtp_date_picker_text_highlighted)
        }
        mSelectedDayTextColor = ContextCompat.getColor(context, R.color.mdtp_white)
        mTodayNumberColor = mController.accentColor
        mMonthTitleColor = ContextCompat.getColor(context, R.color.mdtp_white)
        mStringBuilder = StringBuilder(50)
        mFormatter = Formatter(mStringBuilder, mController.locale)
        MINI_DAY_NUMBER_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.mdtp_day_number_size)
        MONTH_LABEL_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.mdtp_month_label_size)
        MONTH_DAY_LABEL_TEXT_SIZE = res.getDimensionPixelSize(R.dimen.mdtp_month_day_label_text_size)
        monthHeaderSize = res.getDimensionPixelOffset(R.dimen.mdtp_month_list_item_header_height)
        DAY_SELECTED_CIRCLE_SIZE = res
            .getDimensionPixelSize(R.dimen.mdtp_day_number_select_circle_radius)
        mRowHeight = (res.getDimensionPixelOffset(R.dimen.mdtp_date_picker_view_animator_height)
            - monthHeaderSize) / MAX_NUM_ROWS

        // Set up accessibility components.
        mTouchHelper = monthViewTouchHelper
        ViewCompat.setAccessibilityDelegate(this, mTouchHelper)
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
        mLockAccessibilityDelegate = true

        // Sets up any standard paints that will be used
        initView()
    }
}