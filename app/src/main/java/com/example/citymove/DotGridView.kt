package com.example.citymove

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class DotGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x12FFFFFF.toInt()   // trắng ~7% opacity — mờ như ảnh
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
    }

    private val cellSizeDp = 30f    // kích thước 1 ô lưới (dp)
    private var cellSizePx = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSizePx = cellSizeDp * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ đường dọc
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
            x += cellSizePx
        }

        // Vẽ đường ngang
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
            y += cellSizePx
        }
    }
}