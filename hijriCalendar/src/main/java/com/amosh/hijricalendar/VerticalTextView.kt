package com.amosh.hijricalendar

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView


/**
 * TextView that renders it's contents vertically. (Just using rotate doesn't work because onMeasure
 * happens before the View is rotated causing incorrect View boundaries)
 */
class VerticalTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    private var topDown = false
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(canvas: Canvas) {
        val textPaint = paint
        textPaint.color = currentTextColor
        textPaint.drawableState = drawableState
        canvas.save()
        when {
            topDown -> {
                canvas.translate(width.toFloat(), 0f)
                canvas.rotate(90f)
            }
            else -> {
                canvas.translate(0f, height.toFloat())
                canvas.rotate(-90f)
            }
        }
        canvas.translate(compoundPaddingLeft.toFloat(), extendedPaddingTop.toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    init {
        val gravity = gravity
        topDown = when {
            Gravity.isVertical(gravity) && gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.BOTTOM -> {
                setGravity(gravity and Gravity.HORIZONTAL_GRAVITY_MASK or Gravity.TOP)
                false
            }
            else -> true
        }
    }
}
