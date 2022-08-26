package com.amosh.hijricalendar.date.gregorian

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import android.widget.ListView
import com.amosh.hijricalendar.Utils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * This displays a list of months in a calendar format with selectable days.
 */
abstract class DayPickerView : ListView, OnScrollListener, GregorianDatePickerDialog.OnDateChangedListener {
    // These affect the scroll speed and feel
    private var mFriction = 1.0f
    private var mContext: Context? = null
    var mHandler: Handler? = null

    // highlighted time
    private var mSelectedDay: MonthAdapter.CalendarDay? = null
    private var mAdapter: MonthAdapter? = null
    private var mTempDay: MonthAdapter.CalendarDay? = null

    // which month should be displayed/highlighted [0-11]
    private var mCurrentMonthDisplayed = 0

    // used for tracking during a scroll
    private var mPreviousScrollPosition: Long = 0

    // used for tracking what state listview is in
    var mPreviousScrollState: Int = OnScrollListener.SCROLL_STATE_IDLE

    // used for tracking what state listview is in
    var mCurrentScrollState: Int = OnScrollListener.SCROLL_STATE_IDLE
    private lateinit var mController: DatePickerController
    private var mPerformingScroll = false

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, controller: DatePickerController) : super(context) {
        init(context)
        setController(controller)
    }

    private fun setController(controller: DatePickerController) {
        mController = controller
        mController.registerOnDateChangedListener(this)
        mSelectedDay = MonthAdapter.CalendarDay(mController.timeZone)
        mTempDay = MonthAdapter.CalendarDay(mController.timeZone)
        refreshAdapter()
        onDateChanged()
    }

    fun init(context: Context) {
        mHandler = Handler(Looper.getMainLooper())
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        isDrawSelectorOnTop = false
        mContext = context
        setUpListView()
    }

    fun onChange() {
        refreshAdapter()
    }

    /**
     * Creates a new adapter if necessary and sets up its parameters. Override
     * this method to provide a custom adapter.
     */
    private fun refreshAdapter() {
        if (mAdapter == null) {
            mAdapter = createMonthAdapter(context, mController)
        }

        mAdapter!!.selectedDay = mSelectedDay
        // refresh the view with the new parameters
        adapter = mAdapter
    }

    abstract fun createMonthAdapter(
        context: Context,
        controller: DatePickerController,
    ): MonthAdapter

    /*
     * Sets all the required fields for the list view. Override this method to
     * set a different list view behavior.
     */
    private fun setUpListView() {
        // Transparent background on scroll
        cacheColorHint = 0
        // No dividers
        divider = null
        // Items are clickable
        itemsCanFocus = true
        // The thumb gets in the way, so disable it
        isFastScrollEnabled = false
        isVerticalScrollBarEnabled = false
        setOnScrollListener(this)
        setFadingEdgeLength(0)
        // Make the scrolling behavior nicer
        setFriction(ViewConfiguration.getScrollFriction() * mFriction)
    }

    /**
     * This moves to the specified time in the view. If the time is not already
     * in range it will move the list so that the first of the month containing
     * the time is at the top of the view. If the new time is already in view
     * the list will not be scrolled unless forceScroll is true. This time may
     * optionally be highlighted as selected as well.
     *
     * @param day         The day to move to
     * @param animate     Whether to scroll to the given time or just redraw at the
     * new location
     * @param setSelected Whether to set the given time as selected
     * @return Whether or not the view animated to the new location
     */
    private fun goTo(day: MonthAdapter.CalendarDay?, animate: Boolean, setSelected: Boolean): Boolean {

        // Set the selected day
        if (setSelected) {
            mSelectedDay!!.set(day)
        }
        mTempDay!!.set(day)
        val minMonth = mController.startDate.get(Calendar.MONTH)
        val position = day!!.year - mController.minYear * MonthAdapter.MONTHS_IN_YEAR + day.month - minMonth
        var child: View?
        var i = 0
        var top: Int
        // Find a child that's completely in the view
        do {
            child = getChildAt(i++)
            if (child == null) {
                break
            }
            top = child.top
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "child at " + (i - 1) + " has top " + top)
            }
        } while (top < 0)

        // Compute the first and last position visible
        if (setSelected) {
            mAdapter!!.selectedDay = mSelectedDay
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "GoTo position $position")
        }
        // Check if the selected day is now outside of our visible range
        // and if so scroll to the month that contains it
        setMonthDisplayed(mTempDay)
        mPreviousScrollState = OnScrollListener.SCROLL_STATE_FLING
        if (animate) {
            smoothScrollToPositionFromTop(
                position, LIST_TOP_OFFSET, GOTO_SCROLL_DURATION)
            return true
        } else {
            postSetSelection(position)
        }
        return false
    }

    fun postSetSelection(position: Int) {
        clearFocus()
        post { setSelection(position) }
        onScrollStateChanged(this, OnScrollListener.SCROLL_STATE_IDLE)
    }

    /**
     * Updates the title and selected month if the view has moved to a new
     * month.
     */
    override fun onScroll(
        view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int,
    ) {

        val child = if (view.childCount > 0) {
            view.getChildAt(0) as MonthView
        } else return

        // Figure out where we are
        val currScroll: Long = (view.firstVisiblePosition * child.height - child.bottom).toLong()
        mPreviousScrollPosition = currScroll
        mPreviousScrollState = mCurrentScrollState
    }

    /**
     * Sets the month displayed at the top of this view based on time. Override
     * to add custom events when the title is changed.
     */
    private fun setMonthDisplayed(date: MonthAdapter.CalendarDay?) {
        mCurrentMonthDisplayed = date!!.month
        invalidateViews()
    }

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
        // use a post to prevent re-entering onScrollStateChanged before it
        // exits
        mScrollStateChangedRunnable.doScrollStateChange(scrollState)
    }

    private var mScrollStateChangedRunnable: ScrollStateRunnable = ScrollStateRunnable()

    inner class ScrollStateRunnable : Runnable {
        private var mNewState = 0

        /**
         * Sets up the runnable with a short delay in case the scroll state
         * immediately changes again.
         *
         * @param scrollState The new state it changed to
         */
        fun doScrollStateChange(scrollState: Int) {
            mHandler!!.removeCallbacks(this)
            mNewState = scrollState
            mHandler!!.postDelayed(this, SCROLL_CHANGE_DELAY.toLong())
        }

        override fun run() {
            mCurrentScrollState = mNewState
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG,
                    "new scroll state: $mNewState old state: $mPreviousScrollState")
            }
            // Fix the position after a scroll or a fling ends
            if (mNewState == OnScrollListener.SCROLL_STATE_IDLE && mPreviousScrollState != OnScrollListener.SCROLL_STATE_IDLE && mPreviousScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                mPreviousScrollState = mNewState
                var i = 0
                var child = getChildAt(i)
                while (child != null && child.bottom <= 0) {
                    child = getChildAt(++i)
                }
                if (child == null) {
                    // The view is no longer visible, just return
                    return
                }
                val firstPosition = firstVisiblePosition
                val lastPosition = lastVisiblePosition
                val scroll = firstPosition != 0 && lastPosition != count - 1
                val top = child.top
                val bottom = child.bottom
                val midpoint = height / 2
                if (scroll && top < LIST_TOP_OFFSET) {
                    if (bottom > midpoint) {
                        smoothScrollBy(top, GOTO_SCROLL_DURATION)
                    } else {
                        smoothScrollBy(bottom, GOTO_SCROLL_DURATION)
                    }
                }
            } else {
                mPreviousScrollState = mNewState
            }
        }
    }

    /**
     * Gets the position of the view that is most prominently displayed within the list view.
     */
    val mostVisiblePosition: Int
        get() {
            val firstPosition = firstVisiblePosition
            val height = height
            var maxDisplayedHeight = 0
            var mostVisibleIndex = 0
            var i = 0
            var bottom = 0
            while (bottom < height) {
                val child = getChildAt(i) ?: break
                bottom = child.bottom
                val displayedHeight = min(bottom, height) - max(0, child.top)
                if (displayedHeight > maxDisplayedHeight) {
                    mostVisibleIndex = i
                    maxDisplayedHeight = displayedHeight
                }
                i++
            }
            return firstPosition + mostVisibleIndex
        }

    override fun onDateChanged() {
        goTo(mController.selectedDay, animate = false, setSelected = true)
    }

    /**
     * Attempts to return the date that has accessibility focus.
     *
     * @return The date that has accessibility focus, or `null` if no date
     * has focus.
     */
    private fun findAccessibilityFocus(): MonthAdapter.CalendarDay? {
        val childCount = childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is MonthView) {
                val focus: MonthAdapter.CalendarDay? = child.accessibilityFocus
                if (focus != null) {
                    return focus
                }
            }
        }
        return null
    }

    /**
     * Attempts to restore accessibility focus to a given date. No-op if
     * `day` is `null`.
     *
     * @param day The date that should receive accessibility focus
     * @return `true` if focus was restored
     */
    private fun restoreAccessibilityFocus(day: MonthAdapter.CalendarDay?): Boolean {
        if (day == null) {
            return false
        }
        val childCount = childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is MonthView) {
                if (child.restoreAccessibilityFocus(day)) {
                    return true
                }
            }
        }
        return false
    }

    override fun layoutChildren() {
        val focusedDay = findAccessibilityFocus()
        super.layoutChildren()
        if (mPerformingScroll) {
            mPerformingScroll = false
        } else {
            restoreAccessibilityFocus(focusedDay)
        }
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.itemCount = -1
    }

    /**
     * Necessary for accessibility, to ensure we support "scrolling" forward and backward
     * in the month list.
     */
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
    }

    /**
     * When scroll forward/backward events are received, announce the newly scrolled-to month.
     */
    @SuppressLint("NewApi")
    override fun performAccessibilityAction(action: Int, arguments: Bundle): Boolean {
        if (action != AccessibilityNodeInfo.ACTION_SCROLL_FORWARD &&
            action != AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        ) {
            return super.performAccessibilityAction(action, arguments)
        }

        // Figure out what month is showing.
        val firstVisiblePosition = firstVisiblePosition
        val minMonth = mController.startDate.get(Calendar.MONTH)
        val month: Int = (firstVisiblePosition + minMonth) % MonthAdapter.MONTHS_IN_YEAR
        val year: Int = (firstVisiblePosition + minMonth) / MonthAdapter.MONTHS_IN_YEAR + mController.minYear
        val day = MonthAdapter.CalendarDay(year, month, 1)

        // Scroll either forward or backward one month.
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            day.month++
            if (day.month == 12) {
                day.month = 0
                day.year++
            }
        } else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            val firstVisibleView = getChildAt(0)
            // If the view is fully visible, jump one month back. Otherwise, we'll just jump
            // to the first day of first visible month.
            if (firstVisibleView != null && firstVisibleView.top >= -1) {
                // There's an off-by-one somewhere, so the top of the first visible item will
                // actually be -1 when it's at the exact top.
                day.month--
                if (day.month == -1) {
                    day.month = 11
                    day.year--
                }
            }
        }

        // Go to that month.
        Utils.tryAccessibilityAnnounce(this, getMonthAndYearString(day, mController.locale))
        goTo(day, animate = true, setSelected = false)
        mPerformingScroll = true
        return true
    }

    companion object {
        private const val TAG = "MonthFragment"

        // How long the GoTo fling animation should last
        const val GOTO_SCROLL_DURATION = 250

        // How long to wait after receiving an onScrollStateChanged notification
        // before acting on it
        const val SCROLL_CHANGE_DELAY = 40

        var LIST_TOP_OFFSET = -1 // so that the top line will be
        private val YEAR_FORMAT = SimpleDateFormat("yyyy", Locale.ROOT)
        private fun getMonthAndYearString(day: MonthAdapter.CalendarDay, locale: Locale): String {
            val cal = Calendar.getInstance()
            cal[day.year, day.month] = day.day
            var sbuf = ""
            sbuf += cal.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)
            sbuf += " "
            sbuf += YEAR_FORMAT.format(cal.time)
            return sbuf
        }
    }
}