package com.amosh.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.FragmentPagerAdapter
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class MainActivity : AppCompatActivity() {
    var viewPager: ViewPager? = null
    var adapter: PickerAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adapter = PickerAdapter(supportFragmentManager)
        viewPager = findViewById<View>(R.id.pager) as ViewPager
        viewPager!!.adapter = adapter
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar)
        val tabLayout = findViewById<View>(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)
        for (i in 0 until adapter!!.count) tabLayout.getTabAt(i)!!.setText(adapter!!.getTitle(i))
    }

    class PickerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
        var datePickerFragment: Fragment = GregorianDatePickerFragment()
        var hijriPickerFragment: Fragment = HijriDatePickerFragment()
        override fun getCount(): Int {
            return NUM_PAGES
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> datePickerFragment
                1 -> hijriPickerFragment
                else -> datePickerFragment
            }
        }

        fun getTitle(position: Int): Int {
            return when (position) {
                0 -> R.string.tab_title_gregorian_date
                1 -> R.string.tab_title_hijri_date
                else -> R.string.tab_title_gregorian_date
            }
        }

        companion object {
            private const val NUM_PAGES = 2
        }

    }
}