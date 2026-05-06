package com.ctis487.smartwardrobe.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CircularColorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private var circleColor: Int = Color.GRAY

    fun setColor(colorString: String) {
        try {
            circleColor = Color.parseColor(colorString)
        } catch (e: Exception) {
            circleColor = Color.GRAY
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = circleColor
        val radius = Math.min(width, height) / 2f
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
    }
}
