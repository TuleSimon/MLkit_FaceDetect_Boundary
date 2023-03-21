package com.simon.facecapture.camera

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.simon.facecapture.R

class OverlayPosition(var x: Float, var y: Float, var r: Float)

class OverlayView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint: Paint = Paint()
    private var holePaint: Paint = Paint()
    private var bitmap: Bitmap? = null
    private var layer: Canvas? = null
    private var border: Paint = Paint()

    //position of hole
    var holePosition: OverlayPosition = OverlayPosition(0.0f, 0.0f, 0.0f)
        set(value) {
            field = value
            //redraw
            this.invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bitmap == null) {
            configureBitmap()
        }


        //  draw background
        layer?.drawRect(0.0f, 0.0f, width.toFloat(), height.toFloat(), paint)
//        //draw hole
        layer?.drawCircle((width / 2).toFloat(), (height / 3).toFloat(), 450f, border)
        layer?.drawCircle((width / 2).toFloat(), (height / 3).toFloat(), 450f, holePaint.apply { alpha = 0 })
        //draw bitmap
        canvas.drawBitmap(bitmap!!, 0.0f, 0.0f, paint);
    }

    private fun configureBitmap() {
        //create bitmap and layer
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        layer = Canvas(bitmap!!)
    }

    init {

        val a: TypedArray =
            getContext()
                .obtainStyledAttributes(attrs,
                    R.styleable.OverlayView,
                    0, 0);

        val color = a.getColor(R.styleable.OverlayView_circleColor,  Color.parseColor("#FFFFFF"))

        //do something with str


        //do something with str
        a.recycle()

        //configure background color
        val backgroundAlpha = 0.7
        paint.color = ColorUtils.setAlphaComponent(context?.let {
            ContextCompat.getColor(
                it,
                R.color.overlay
            )
        }!!, (255 * backgroundAlpha).toInt())

        border.color =color
        border.strokeWidth = 30F
        border.style = Paint.Style.STROKE
        border.isAntiAlias = true
        border.isDither = true

        //configure hole color & mode
        holePaint.color = ContextCompat.getColor(context, android.R.color.transparent)

        holePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)


    }


    public fun setCircleColor(color: Int){
        border.color = color
        invalidate()
    }
}