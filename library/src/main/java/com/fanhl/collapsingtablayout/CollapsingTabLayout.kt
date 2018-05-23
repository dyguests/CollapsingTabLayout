package com.fanhl.collapsingtablayout

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Canvas
import android.support.v4.util.Pools
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.*
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import java.lang.ref.WeakReference
import java.util.*

/**
 * 可展开成多行的TabLayout
 *
 * @author fanhl
 */
class CollapsingTabLayout(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : /*fixme 这里可能要自定义*/FrameLayout(context, attrs, defStyleAttr) {
    private val mTabs = ArrayList<Tab>()
    private var mSelectedTab: Tab? = null

    private val mTabStrip: SlidingTabStrip

    private val mRequestedTabMinWidth: Int
    private val mRequestedTabMaxWidth: Int
    private val mScrollableTabMinWidth: Int

    internal var mMode: Int = 0

    internal var mViewPager: ViewPager? = null
    private var mPagerAdapter: PagerAdapter? = null
    private var mPagerAdapterObserver: DataSetObserver? = null
    private var mPageChangeListener: TabLayoutOnPageChangeListener? = null

    // Pool we use as a simple RecyclerBin
    private val mTabViewPool = Pools.SimplePool<TabView>(12)

    init {
        ThemeUtils.checkAppCompatTheme(context)

        // Comment by fanhl 这个是不是不需要啊?
        // Disable the Scroll Bar
        isHorizontalScrollBarEnabled = false

        // Add the TabStrip
        mTabStrip = SlidingTabStrip(context)

        super.addView(mTabStrip, 0, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(0, 0, 100, 100)
        }
    }

    internal fun setScrollPosition(position: Int, positionOffset: Float, updateSelectedText: Boolean, updateIndicatorPosition: Boolean) {
        // FIXME: 2018/5/22 fanhl
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab Tab to add
     */
    fun addTab(tab: Tab) {
        addTab(tab, mTabs.isEmpty())
    }

    /**
     * Add a tab to this layout. The tab will be inserted at `position`.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     */
    fun addTab(tab: Tab, position: Int) {
        addTab(tab, position, mTabs.isEmpty())
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     *
     * @param tab Tab to add
     * @param setSelected True if the added tab should become the selected tab.
     */
    fun addTab(tab: Tab, setSelected: Boolean) {
        addTab(tab, mTabs.size, setSelected)
    }

    /**
     * Add a tab to this layout. The tab will be inserted at `position`.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     * @param setSelected True if the added tab should become the selected tab.
     */
    fun addTab(tab: Tab, position: Int, setSelected: Boolean) {
        if (tab.mParent !== this) {
            throw IllegalArgumentException("Tab belongs to a different TabLayout.")
        }
        configureTab(tab, position)
        addTabView(tab)

        if (setSelected) {
            tab.select()
        }
    }

    fun newTab(): Tab {
        var tab = sTabPool.acquire()
        if (tab == null) {
            tab = Tab()
        }
        tab!!.mParent = this
        tab!!.mView = createTabView(tab!!)
        return tab
    }

    private fun getTabCount(): Int {
        // FIXME: 2018/5/22 fanhl
        return 3
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

    private fun removeAllTabs() {
        // Remove all the views
        for (i in mTabStrip.getChildCount() - 1 downTo 0) {
            removeTabViewAt(i)
        }

        // FIXME: 2018/5/23 fanhl
    }

    fun setupWithViewPager(viewPager: ViewPager?, autoRefresh: Boolean = true, implicitSetup: Boolean = false) {
        if (mViewPager != null) {
            // If we've already been setup with a ViewPager, remove us from it
            if (mPageChangeListener != null) {
                mViewPager?.removeOnPageChangeListener(mPageChangeListener!!)
            }
            // FIXME: 2018/5/22 fanhl mAdapterChangeListener
        }

        // FIXME: 2018/5/23 fanhl
//        if (mCurrentVpSelectedListener != null) {
//            // If we already have a tab selected listener for the ViewPager, remove it
//            removeOnTabSelectedListener(mCurrentVpSelectedListener)
//            mCurrentVpSelectedListener = null
//        }


        if (viewPager != null) {
            mViewPager = viewPager

            // Add our custom OnPageChangeListener to the ViewPager
            if (mPageChangeListener == null) {
                mPageChangeListener = TabLayoutOnPageChangeListener(this)
            }
            mPageChangeListener?.reset()
            viewPager.addOnPageChangeListener(mPageChangeListener!!)

            // FIXME: 2018/5/22 fanhl

            val adapter = viewPager.adapter
            if (adapter != null) {
                // Now we'll populate ourselves from the pager adapter, adding an observer if
                // autoRefresh is enabled
                setPagerAdapter(adapter, autoRefresh)
            }

        }
    }

    internal fun setPagerAdapter(adapter: PagerAdapter?, addObserver: Boolean) {
        if (mPagerAdapter != null && mPagerAdapterObserver != null) {
            // If we already have a PagerAdapter, unregister our observer
            mPagerAdapter!!.unregisterDataSetObserver(mPagerAdapterObserver!!)
        }

        mPagerAdapter = adapter

        if (addObserver && adapter != null) {
            // Register our observer on the new adapter
            if (mPagerAdapterObserver == null) {
                mPagerAdapterObserver = PagerAdapterObserver()
            }
            adapter.registerDataSetObserver(mPagerAdapterObserver!!)
        }

        // Finally make sure we reflect the new adapter
        populateFromPagerAdapter()
    }

    private fun populateFromPagerAdapter() {
        removeAllTabs()

        if (mPagerAdapter != null) {
            val adapterCount = mPagerAdapter!!.count
            for (i in 0 until adapterCount) {
                addTab(newTab().setText(mPagerAdapter!!.getPageTitle(i)), false)
            }

            // Make sure we reflect the currently set ViewPager item
            if (mViewPager != null && adapterCount > 0) {
                val curItem = mViewPager!!.currentItem
                if (curItem != getSelectedTabPosition() && curItem < getTabCount()) {
                    selectTab(getTabAt(curItem))
                }
            }
        }
    }

    private fun createTabView(tab: Tab): TabView {
        var tabView: TabView? = if (mTabViewPool != null) mTabViewPool.acquire() else null
        if (tabView == null) {
            tabView = TabView(context)
        }
        tabView.setTab(tab)
        tabView.isFocusable = true
        tabView.minimumWidth = getTabMinWidth()
        return tabView
    }

    private fun configureTab(tab: Tab, position: Int) {
        tab.setPosition(position)
        mTabs.add(position, tab)

        val count = mTabs.size
        for (i in position + 1 until count) {
            mTabs[i].setPosition(i)
        }
    }

    private fun addTabView(tab: Tab) {
        val tabView = tab.mView
        mTabStrip.addView(tabView, tab.getPosition(), createLayoutParamsForTabs())
    }

    override fun addView(child: View?) {
        super.addView(child)
    }

    private fun createLayoutParamsForTabs(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT)

        // FIXME: 2018/5/23 fanhl
//        updateTabViewLayoutParams(lp)
        return lp
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

    private fun removeTabViewAt(position: Int) {
        // FIXME: 2018/5/23 fanhl
    }

    private fun selectTab(tabAt: Tab?, updateIndicator: Boolean = true) {
        // FIXME: 2018/5/22 fanhl
    }

    private fun getTabMinWidth(): Int {
        if (mRequestedTabMinWidth != INVALID_WIDTH) {
            // If we have been given a min width, use it
            return mRequestedTabMinWidth
        }
        // Else, we'll use the default value
        return if (mMode == MODE_SCROLLABLE) mScrollableTabMinWidth else 0
    }

    companion object {
        private val DEFAULT_HEIGHT_WITH_TEXT_ICON = 72 // dps
        internal val DEFAULT_GAP_TEXT_ICON = 8 // dps
        private val INVALID_WIDTH = -1
        private val DEFAULT_HEIGHT = 48 // dps
        private val TAB_MIN_WIDTH_MARGIN = 56 //dps
        internal val FIXED_WRAP_GUTTER_MIN = 16 //dps
        internal val MOTION_NON_ADJACENT_OFFSET = 24

        private val ANIMATION_DURATION = 300

        private val sTabPool = Pools.SynchronizedPool<Tab>(16)

        /**
         * Scrollable tabs display a subset of tabs at any given moment, and can contain longer tab
         * labels and a larger number of tabs. They are best used for browsing contexts in touch
         * interfaces when users don’t need to directly compare the tab labels.
         *
         * @see .setTabMode
         * @see .getTabMode
         */
        val MODE_SCROLLABLE = 0
    }

    class Tab {

    }

    internal inner class TabView(context: Context) : LinearLayout(context) {

    }

    /**
     * fixme 这里要改布局
     */
    private class SlidingTabStrip(context: Context) : LinearLayout(context) {
        // FIXME: 2018/5/23 fanhl

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)

        }
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
            val tabLayout = mTabLayoutRef.get()
            if (tabLayout != null) {
                // Only update the text selection if we're not settling, or we are settling after
                // being dragged
                val updateText = mScrollState != SCROLL_STATE_SETTLING || mPreviousScrollState == SCROLL_STATE_DRAGGING
                // Update the indicator if we're not settling after being idle. This is caused
                // from a setCurrentItem() call and will be handled by an animation from
                // onPageSelected() instead.
                val updateIndicator = !(mScrollState == SCROLL_STATE_SETTLING && mPreviousScrollState == SCROLL_STATE_IDLE)
                tabLayout.setScrollPosition(position, positionOffset, updateText, updateIndicator)
            }
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

        fun reset() {
            // FIXME: 2018/5/22 fanhl
        }

    }

    private inner class PagerAdapterObserver internal constructor() : DataSetObserver() {

        override fun onChanged() {
            populateFromPagerAdapter()
        }

        override fun onInvalidated() {
            populateFromPagerAdapter()
        }
    }
}
