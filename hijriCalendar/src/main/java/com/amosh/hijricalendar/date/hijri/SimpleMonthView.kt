package com.amosh.hijricalendar.date.hijri

import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.util.AttributeSet

class SimpleMonthView(context: Context, attr: AttributeSet?, controller: DatePickerController) : MonthView(context, attr, controller) {
    override fun drawMonthDay(
        canvas: Canvas, year: Int, month: Int, day: Int,
        x: Int, y: Int, startX: Int, stopX: Int, startY: Int, stopY: Int
    ) {
        if (mSelectedDay == day) {
            canvas.drawCircle(x.toFloat(),
                (y - MINI_DAY_NUMBER_TEXT_SIZE / 3).toFloat(),
                DAY_SELECTED_CIRCLE_SIZE.toFloat(),
                mSelectedCirclePaint!!)
        }
        if (isHighlighted(year, month, day)) {
            mMonthNumPaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } else {
            mMonthNumPaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // If we have a mindate or maxdate, gray out the day number if it's outside the range.
        if (mController.isOutOfRange(year, month, day)) {
            mMonthNumPaint!!.color = mDisabledDayTextColor
        } else if (mSelectedDay == day) {
            mMonthNumPaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            mMonthNumPaint!!.color = mSelectedDayTextColor
        } else if (mHasToday && mToday == day) {
            mMonthNumPaint!!.color = mTodayNumberColor
        } else {
            mMonthNumPaint!!.color = if (isHighlighted(year, month, day)) mHighlightedDayTextColor else mDayTextColor
        }
        canvas.drawText(String.format("%d", day), x.toFloat(), y.toFloat(), mMonthNumPaint!!)
    }
}