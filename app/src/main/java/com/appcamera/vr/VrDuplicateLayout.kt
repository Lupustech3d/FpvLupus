package com.appcamera.vr

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout

class VrDuplicateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var videoView: TextureView? = null

    init {
        setWillNotDraw(false)
        clipChildren = true
        clipToPadding = true
    }

    fun setVideoView(textureView: TextureView) {
        if (videoView === textureView) return
        removeAllViews()
        videoView = textureView
        addView(
            textureView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val child = videoView
        if (child == null || child.visibility != VISIBLE) {
            super.dispatchDraw(canvas)
            return
        }

        val halfWidth = width / 2
        if (halfWidth <= 0 || height <= 0) {
            super.dispatchDraw(canvas)
            return
        }

        val saveLeft = canvas.save()
        canvas.clipRect(0, 0, halfWidth, height)
        canvas.scale(0.5f, 1f)
        drawChild(canvas, child, drawingTime)
        canvas.restoreToCount(saveLeft)

        val saveRight = canvas.save()
        canvas.clipRect(halfWidth, 0, width, height)
        canvas.translate(halfWidth.toFloat(), 0f)
        canvas.scale(0.5f, 1f)
        drawChild(canvas, child, drawingTime)
        canvas.restoreToCount(saveRight)
    }
}
