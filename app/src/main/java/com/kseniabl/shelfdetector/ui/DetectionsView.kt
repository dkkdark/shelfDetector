package com.kseniabl.shelfdetector.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.kseniabl.shelfdetector.SDetector
import kotlin.math.min

class DetectionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boundingBoxes: MutableList<RectF> = mutableListOf()
    private val shelves: MutableList<RectF> = mutableListOf()
    private var topPadding = 0f
    private var leftPadding = 0f
    private var image: Bitmap? = null

    private val boundingBoxesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.RED
    }

    private val shelvesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.GREEN
    }

    fun setBoundingBoxes(boundingBoxes: List<SDetector.Rectangle>, image: Bitmap) {
        this.boundingBoxes.clear()
        this.shelves.clear()
        boundingBoxes.forEach {
            val scale = getScale(image)
            this.image = image
            this.topPadding = (height - image.height * scale) / 2
            this.leftPadding =  (width - image.width * scale) / 2

            val left = it.left * image.width * scale
            val right = it.right * image.width * scale
            val top = it.top * image.height * scale
            val bottom = it.bottom * image.height * scale

            val rec = RectF(left, top, right, bottom)
            this.boundingBoxes.add(rec)
        }
        if (boundingBoxes.size > 2) {
            shelfDetection()
        }

        postInvalidate()
    }

    private fun getScale(image: Bitmap): Float {
        val xScale = 1f * width / image.width
        val yScale = 1f * height / image.height
        return min(xScale, yScale)
    }

    private fun shelfDetection() {
        val sortedBoxes = boundingBoxes.sortedBy { it.top }
        val groups = mutableListOf(mutableListOf(sortedBoxes[0]))

        for (i in 1 until sortedBoxes.size) {
            val el = sortedBoxes[i]
            if (el.top - groups.last().last().top <= 110) {
                groups.last().add(el)
            } else {
                groups.add(mutableListOf(el))
            }
        }

        groups.forEach {
            val shelf = findAverageShelfSize(it)
            shelves.add(shelf)
        }
    }

    private fun findAverageShelfSize(list: MutableList<RectF>): RectF {
        val left = list.minBy { it.left }.left
        val top = list.minBy { it.top }.top
        val right = list.maxBy { it.right }.right
        val bottom = list.maxBy { it.bottom }.bottom
        return RectF(left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(leftPadding, topPadding)

        boundingBoxes.forEach {
            canvas.drawRect(it, boundingBoxesPaint)
        }

        shelves.forEach {
            canvas.drawRect(it, shelvesPaint)
        }
    }

}