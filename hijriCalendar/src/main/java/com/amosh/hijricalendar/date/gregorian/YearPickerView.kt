package com.amosh.hijricalendar.date.gregorian

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.amosh.hijricalendar.R

/**
 * Displays a selectable list of years.
 */
class YearPickerView(context: Context?, private val mController: DatePickerController) : ListView(context), AdapterView.OnItemClickListener,
    GregorianDatePickerDialog.OnDateChangedListener {
    private var mAdapter: YearAdapter? = null
    private val mViewSize: Int
    private val mChildSize: Int
    private var mSelectedView: TextViewWithCircularIndicator? = null
    private fun init() {
        mAdapter = YearAdapter(mController.minYear, mController.maxYear)
        adapter = mAdapter
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        mController.tryVibrate()
        val clickedView = view as TextViewWithCircularIndicator
        if (clickedView !== mSelectedView) {
            if (mSelectedView != null) {
                mSelectedView!!.drawIndicator(false)
                mSelectedView?.requestLayout()
            }
            clickedView.drawIndicator(true)
            clickedView.requestLayout()
            mSelectedView = clickedView
        }
        mController.onYearSelected(getYearFromTextView(clickedView))
        mAdapter?.notifyDataSetChanged()
    }

    private inner class YearAdapter(minYear: Int, maxYear: Int) : BaseAdapter() {
        private val mMinYear: Int
        private val mMaxYear: Int
        override fun getCount(): Int = mMaxYear - mMinYear + 1

        override fun getItem(position: Int): Any {
            return mMinYear + position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v: TextViewWithCircularIndicator
            if (convertView != null) {
                v = convertView as TextViewWithCircularIndicator
            } else {
                v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.hdp_mdtp_year_label_text_view, parent, false) as TextViewWithCircularIndicator
                v.setAccentColor(mController.accentColor, mController.isThemeDark)
            }
            val year = mMinYear + position
            val selected = mController.selectedDay.year == year
            v.text = String.format(mController.locale, "%d", year)
            v.drawIndicator(selected)
            v.requestLayout()
            if (selected) {
                mSelectedView = v
            }
            return v
        }

        init {
            require(minYear <= maxYear) { "minYear > maxYear" }
            mMinYear = minYear
            mMaxYear = maxYear
        }
    }

    private fun postSetSelectionCentered(position: Int) {
        postSetSelectionFromTop(position, mViewSize / 2 - mChildSize / 2)
    }

    fun postSetSelectionFromTop(position: Int, offset: Int) {
        post {
            setSelectionFromTop(position, offset)
            requestLayout()
        }
    }

    val firstPositionOffset: Int
        get() {
            val firstChild = getChildAt(0) ?: return 0
            return firstChild.top
        }

    override fun onDateChanged() {
        mAdapter?.notifyDataSetChanged()
        postSetSelectionCentered(mController.selectedDay.year - mController.minYear)
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            event.fromIndex = 0
            event.toIndex = 0
        }
    }

    companion object {
        private const val TAG = "YearPickerView"
        private fun getYearFromTextView(view: TextView): Int {
            return Integer.valueOf(view.text.toString())
        }
    }

    init {
        mController.registerOnDateChangedListener(this)
        val frame: ViewGroup.LayoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT)
        layoutParams = frame
        val res = context!!.resources
        mViewSize = res.getDimensionPixelOffset(R.dimen.mdtp_date_picker_view_animator_height)
        mChildSize = res.getDimensionPixelOffset(R.dimen.mdtp_year_label_height)
        isVerticalFadingEdgeEnabled = true
        setFadingEdgeLength(mChildSize / 3)
        init()
        onItemClickListener = this
        selector = StateListDrawable()
        dividerHeight = 0
        onDateChanged()
    }
}