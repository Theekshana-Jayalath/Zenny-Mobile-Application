package com.example.zenny

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class HalfCircleProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 30f
        color = Color.LTGRAY
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 30f
        color = Color.parseColor("#2887b0") // Or any color you prefer
        strokeCap = Paint.Cap.ROUND
    }

    fun setProgress(value: Int) {
        progress = value.coerceIn(0, 100)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Force a 2:1 aspect ratio based on the width to ensure it's a perfect semicircle
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width / 2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val strokeWidth = backgroundPaint.strokeWidth
        // The drawing area is inset by half the stroke width to avoid clipping
        val drawingRect = RectF(
            paddingLeft + strokeWidth / 2f,
            paddingTop + strokeWidth / 2f,
            width - paddingRight - strokeWidth / 2f,
            height - paddingBottom - strokeWidth / 2f
        )

        val centerX = drawingRect.centerX()
        val centerY = height.toFloat() - paddingBottom
        val radius = drawingRect.width() / 2f

        val oval = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Draw the gray background semicircle
        canvas.drawArc(oval, 180f, 180f, false, backgroundPaint)

        // Draw the blue progress arc
        val sweepAngle = (progress / 100f) * 180f
        canvas.drawArc(oval, 180f, sweepAngle, false, progressPaint)
    }
}
