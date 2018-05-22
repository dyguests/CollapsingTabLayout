package com.fanhl.collapsingtablayout

import android.content.Context
import android.graphics.Canvas
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * 可展开成多行的TabLayout
 *
 * @author fanhl
 */
class CollapsingTabLayout : ViewGroup {
    private val mTabs = ArrayList<Tab>()

    internal var mViewPager: ViewPager? = null
    private var mPageChangeListener: TabLayoutOnPageChangeListener? = null

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
            val child = getChildAt(i)
            child.layout(0, 0, 100, 100)
        }
    }

    private fun selectTab(tabAt: Tab?, updateIndicator: Boolean) {
        // FIXME: 2018/5/22 fanhl
    }


    private fun getTabCount(): Int {
        // FIXME: 2018/5/22 fanhl
        return 1
    }

    private fun getSelectedTabPosition(): Int {
        // FIXME: 2018/5/22 fanhl
        return 0
    }

    /**
     * Returns the tab at the specified index.
     */
    fun getTabAt(index: Int): Tab? {
        return if (index < 0 || index >= getTabCount()) null else mTabs[index]
    }

    fun setupWithViewPager(viewPager: ViewPager?, autoRefresh: Boolean = true, implicitSetup: Boolean = false) {
        if (mViewPager != null) {
            // If we've already been setup with a ViewPager, remove us from it
            if (mPageChangeListener != null) {
                mViewPager?.removeOnPageChangeListener(mPageChangeListener!!)
            }
            // FIXME: 2018/5/22 fanhl mAdapterChangeListener
        }

        if (viewPager != null) {
            mViewPager = viewPager

            // Add our custom OnPageChangeListener to the ViewPager
            if (mPageChangeListener == null) {
                mPageChangeListener = TabLayoutOnPageChangeListener(this)
            }
        }
    }

    companion object {
        /**
         * Indicates that the pager is in an idle, settled state. The current page
         * is fully in view and no animation is in progress.
         */
        val SCROLL_STATE_IDLE = 0

        /**
         * Indicates that the pager is currently being dragged by the user.
         */
        val SCROLL_STATE_DRAGGING = 1

        /**
         * Indicates that the pager is in the process of settling to a final position.
         */
        val SCROLL_STATE_SETTLING = 2
    }

    class TabLayoutOnPageChangeListener(tabLayout: CollapsingTabLayout) : ViewPager.OnPageChangeListener {
        private val mTabLayoutRef: WeakReference<CollapsingTabLayout> = WeakReference(tabLayout)
        private var mPreviousScrollState: Int = 0
        private var mScrollState: Int = 0

        override fun onPageScrollStateChanged(state: Int) {
            mPreviousScrollState = mScrollState
            mScrollState = state
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        }

        override fun onPageSelected(position: Int) {
            val tabLayout = mTabLayoutRef.get()
            if (tabLayout != null && tabLayout.getSelectedTabPosition() != position && position < tabLayout.getTabCount()) {
                // Select the tab, only updating the indicator if we're not being dragged/settled
                // (since onPageScrolled will handle that).
                val updateIndicator = mScrollState == SCROLL_STATE_IDLE || mScrollState == SCROLL_STATE_SETTLING && mPreviousScrollState == SCROLL_STATE_IDLE
                tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator)
            }
        }

    }

    class Tab {

    }
}
