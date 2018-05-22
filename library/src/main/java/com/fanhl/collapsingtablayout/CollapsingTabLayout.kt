package com.fanhl.collapsingtablayout

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * 可展开成多行的TabLayout
 *
 * @author fanhl
 */
class CollapsingTabLayout : ViewGroup {
    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    init {

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val specSizeWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        val specSizeHeight = View.MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(specSizeWidth, specSizeHeight)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            this.measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {

        }
    }

    /**
     * 这个方法应该没什么用
     */
    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
//        val textPaint = TextPaint()
//        textPaint.color = Color.BLACK
//        textPaint.textSize = 20f
//        canvas?.drawText("测试", 10f, 10f, textPaint)
    }

    /**
     * 这个也没用
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }


}
