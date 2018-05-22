package com.fanhl.collapsingtablayout.sample

import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_collapsing.*
import kotlinx.android.synthetic.main.item_view.view.*

class CollapsingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collapsing)
        initData()
    }

    private fun initData() {
        view_pager.adapter = object : PagerAdapter() {
            override fun getCount() = 3

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                return LayoutInflater.from(container.context).inflate(R.layout.item_view, null as ViewGroup?, false).apply {
                    tv_text.text = "$position"
                    container.addView(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                container.removeView(`object` as View)
            }

            override fun isViewFromObject(view: View, `object`: Any) = view == `object`

            override fun getItemPosition(`object`: Any) = POSITION_NONE
        }
    }
}
