package me.juhezi.matrix.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import me.juhezi.matrix.R

class MatrixSweepMaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)

    /**
     * 扫光进度。
     * <= 0 或者 >= 1 表示没有扫光效果
     */
    private var maskPercent: Float = -1f

    var maskBitmap: Bitmap? = null

    fun updateMaskPercent(percent: Float) {
        maskPercent = percent
        invalidate()
    }

    private val defaultMaskBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.matrix_sweep_mask)
    }

    private val mMaskRect = Rect()

    override fun draw(canvas: Canvas?) {
        val internalBitmap = maskBitmap ?: defaultMaskBitmap
        val layer = canvas?.saveLayer(0F, 0F, width.toFloat(), height.toFloat(), null)
        super.draw(canvas)

        val percent = if (isInEditMode) {
            0.5f
        } else {
            maskPercent
        }
        if (percent <= 0 || percent >= 1) {
            return
        }
        val maskWidth = internalBitmap.width
        val maskHeight = internalBitmap.height
        if (maskHeight <= 0 || maskWidth <= 0) {
            return
        }
        val targetHeight = height
        val targetWidth = targetHeight * maskWidth / maskHeight.toFloat()
        val startX = (targetWidth + width) * percent
        mMaskRect.let {
            it.left = (startX - targetWidth).toInt()
            it.right = startX.toInt()
            it.top = 0
            it.bottom = targetHeight
        }

        maskPaint.xfermode = xfermode
        canvas?.drawBitmap(internalBitmap, null, mMaskRect, maskPaint)
        maskPaint.xfermode = null
        layer?.let {
            canvas.restoreToCount(it)
        }
    }
}