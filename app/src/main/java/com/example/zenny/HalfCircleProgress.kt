// app/src/main/java/com/example/zenny/HalfCircleProgress.kt

package com.example.zenny

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

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
        color = Color.BLUE // Or any color you prefer
        strokeCap = Paint.Cap.ROUND
    }
    private val oval = RectF()

    fun setProgress(value: Int) {
        progress = value.coerceIn(0, 100) // Ensure progress is between 0 and 100
        invalidate() // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val strokeWidth = backgroundPaint.strokeWidth

        // Define the bounds for the arc
        oval.set(strokeWidth / 2, strokeWidth / 2, viewWidth - strokeWidth / 2, (viewHeight - strokeWidth / 2) * 2)

        // Draw the background arc
        canvas.drawArc(oval, 180f, 180f, false, backgroundPaint)

        // Draw the progress arc
        val sweepAngle = (progress / 100f) * 180f
        canvas.drawArc(oval, 180f, sweepAngle, false, progressPaint)
    }
}
