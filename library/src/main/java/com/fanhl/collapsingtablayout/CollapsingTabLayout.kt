package com.fanhl.collapsingtablayout

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.design.R
import android.support.v4.util.Pools
import android.support.v4.view.PagerAdapter
import android.support.v4.view.PointerIconCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.*
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.*
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

    // FIXME: 2018/5/23 fanhl 之后这部分可能要改成不通过 TabLayout来设定，而是用TabView来设定
    internal var mTabPaddingStart: Int = 0
    internal var mTabPaddingTop: Int = 0
    internal var mTabPaddingEnd: Int = 0
    internal var mTabPaddingBottom: Int = 0

    internal var mTabTextMultiLineSize: Float = 0F

    internal val mTabBackgroundResId: Int

    internal var mTabMaxWidth = Integer.MAX_VALUE
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

        val a = context.obtainStyledAttributes(attrs, R.styleable.TabLayout, defStyleAttr, R.style.Widget_Design_TabLayout)

        mTabPaddingBottom = a.getDimensionPixelSize(R.styleable.TabLayout_tabPadding, 0)
        mTabPaddingEnd = mTabPaddingBottom
        mTabPaddingTop = mTabPaddingEnd
        mTabPaddingStart = mTabPaddingTop

        mRequestedTabMinWidth = a.getDimensionPixelSize(R.styleable.TabLayout_tabMinWidth, INVALID_WIDTH)
        mRequestedTabMaxWidth = a.getDimensionPixelSize(R.styleable.TabLayout_tabMaxWidth, INVALID_WIDTH)
        mTabBackgroundResId = a.getResourceId(R.styleable.TabLayout_tabBackground, 0)

        a.recycle()

        // TODO add attr for these
        val res = resources
        mTabTextMultiLineSize = res.getDimensionPixelSize(R.dimen.design_tab_text_size_2line).toFloat()
        mScrollableTabMinWidth = res.getDimensionPixelSize(R.dimen.design_tab_scrollable_min_width)

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

    class Tab internal constructor() {
        // FIXME: 2018/5/23 fanhl 这里是否允许自定义？TabView样式

        private var mTag: Any? = null
        private var mIcon: Drawable? = null
        private var mText: CharSequence? = null
        private var mContentDesc: CharSequence? = null
        private var mPosition = INVALID_POSITION
        private var mCustomView: View? = null

        internal var mParent: CollapsingTabLayout? = null
        internal var mView: TabView? = null

        /**
         * @return This Tab's tag object.
         */
        fun getTag(): Any? {
            return mTag
        }

        /**
         * Give this Tab an arbitrary object to hold for later use.
         *
         * @param tag Object to store
         * @return The current instance for call chaining
         */
        fun setTag(tag: Any?): Tab {
            mTag = tag
            return this
        }


        /**
         * Return the icon associated with this tab.
         *
         * @return The tab's icon
         */
        fun getIcon(): Drawable? {
            return mIcon
        }

        /**
         * Set the icon displayed on this tab.
         *
         * @param icon The drawable to use as an icon
         * @return The current instance for call chaining
         */
        fun setIcon(icon: Drawable?): Tab {
            mIcon = icon
            updateView()
            return this
        }

        /**
         * Return the text of this tab.
         *
         * @return The tab's text
         */
        fun getText(): CharSequence? {
            return mText
        }

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not room to display
         * the entire string.
         *
         * @param text The text to display
         * @return The current instance for call chaining
         */
        fun setText(text: CharSequence?): Tab {
            mText = text
            updateView()
            return this
        }

        /**
         * Return the current position of this tab in the action bar.
         *
         * @return Current position, or [.INVALID_POSITION] if this tab is not currently in
         * the action bar.
         */
        fun getPosition(): Int {
            return mPosition
        }

        internal fun setPosition(position: Int) {
            mPosition = position
        }

        /**
         * Returns the custom view used for this tab.
         *
         * @see .setCustomView
         * @see .setCustomView
         */
        fun getCustomView(): View? {
            return mCustomView
        }

        /**
         * Set a custom view to be used for this tab.
         *
         *
         * If the provided view contains a [TextView] with an ID of
         * [android.R.id.text1] then that will be updated with the value given
         * to [.setText]. Similarly, if this layout contains an
         * [ImageView] with ID [android.R.id.icon] then it will be updated with
         * the value given to [.setIcon].
         *
         *
         * @param view Custom view to be used as a tab.
         * @return The current instance for call chaining
         */
        fun setCustomView(view: View?): Tab {
            mCustomView = view
            updateView()
            return this
        }

        /**
         * Select this tab. Only valid if the tab has been added to the action bar.
         */
        fun select() {
            if (mParent == null) {
                throw IllegalArgumentException("Tab not attached to a TabLayout")
            }
            mParent!!.selectTab(this)
        }

        /**
         * Returns true if this tab is currently selected.
         */
        fun isSelected(): Boolean {
            if (mParent == null) {
                throw IllegalArgumentException("Tab not attached to a TabLayout")
            }
            return mParent!!.getSelectedTabPosition() == mPosition
        }


        internal fun updateView() {
            if (mView != null) {
                mView!!.update()
            }
        }

        companion object {
            /**
             * An invalid position for a tab.
             *
             * @see .getPosition
             */
            const val INVALID_POSITION = -1
        }
    }

    internal inner class TabView(context: Context) : LinearLayout(context) {
        // FIXME: 2018/5/23 fanhl 这里是否可以自定义

        private var mTab: Tab? = null
        private var mTextView: TextView? = null
        private var mIconView: ImageView? = null

        private var mCustomView: View? = null
        private var mCustomTextView: TextView? = null
        private var mCustomIconView: ImageView? = null

        private var mDefaultMaxLines = 2

        init {
            if (mTabBackgroundResId != 0) {
                ViewCompat.setBackground(this, AppCompatResources.getDrawable(context, mTabBackgroundResId))
            }
            ViewCompat.setPaddingRelative(this, mTabPaddingStart, mTabPaddingTop, mTabPaddingEnd, mTabPaddingBottom)
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            isClickable = true
            ViewCompat.setPointerIcon(this, PointerIconCompat.getSystemIcon(getContext(), PointerIconCompat.TYPE_HAND))
        }

        fun setTab(tab: Tab?) {
            if (tab != mTab) {
                mTab = tab
                update()
            }
        }

        fun reset() {
            setTab(null)
            isSelected = false
        }

        fun update() {
            val tab = mTab
            val custom = tab?.getCustomView()

            // FIXME: 2018/5/23 fanhl
        }
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
