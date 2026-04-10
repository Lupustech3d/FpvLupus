package com.lupustech.fpvlupus.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class VrDualFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val horizontalInsetRatio = 0.04f
    private val verticalInsetRatio = 0.04f
    private val eyeConvergenceShiftRatio = 0.035f
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val leftRect = RectF()
    private val rightRect = RectF()

    @Volatile
    private var frameBitmap: Bitmap? = null

    init {
        setBackgroundColor(Color.BLACK)
        keepScreenOn = true
    }

    fun setFrame(bitmap: Bitmap) {
        frameBitmap = bitmap
        postInvalidateOnAnimation()
    }

    fun clearFrame() {
        frameBitmap = null
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        val bitmap = frameBitmap ?: return
        if (width <= 0 || height <= 0) return

        val halfWidth = width / 2f
        val horizontalInset = halfWidth * horizontalInsetRatio
        val verticalInset = height * verticalInsetRatio
        val convergenceShift = halfWidth * eyeConvergenceShiftRatio

        leftRect.set(0f, 0f, halfWidth, height.toFloat())
        rightRect.set(halfWidth, 0f, width.toFloat(), height.toFloat())

        leftRect.inset(horizontalInset, verticalInset)
        rightRect.inset(horizontalInset, verticalInset)
        leftRect.offset(convergenceShift, 0f)
        rightRect.offset(-convergenceShift, 0f)

        drawBitmapFitCenter(canvas, bitmap, leftRect)
        drawBitmapFitCenter(canvas, bitmap, rightRect)
    }

    private fun drawBitmapFitCenter(canvas: Canvas, bitmap: Bitmap, target: RectF) {
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()
        if (bitmapWidth <= 0f || bitmapHeight <= 0f) return

        val scale = minOf(target.width() / bitmapWidth, target.height() / bitmapHeight)
        val drawWidth = bitmapWidth * scale
        val drawHeight = bitmapHeight * scale
        val left = target.left + (target.width() - drawWidth) / 2f
        val top = target.top + (target.height() - drawHeight) / 2f

        canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawWidth, top + drawHeight), paint)
    }
}
