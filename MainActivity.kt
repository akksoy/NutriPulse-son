package com.nutripulse.app

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nutripulse.app.databinding.ActivityMainBinding
import com.nutripulse.app.ui.animal.AnimalListFragment
import com.nutripulse.app.ui.feed.FeedFragment
import com.nutripulse.app.ui.ration.RationStudioFragment
import com.nutripulse.app.ui.report.ReportFragment
import com.nutripulse.app.ui.settings.SettingsFragment
import com.nutripulse.app.ui.stock.StockFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            setActiveTab(0)
        }
        setupBottomNav()
    }

    private fun setupBottomNav() {
        binding.tabHome.setOnClickListener    { loadFragment(HomeFragment());          setActiveTab(0) }
        binding.tabFeed.setOnClickListener    { loadFragment(FeedFragment());          setActiveTab(1) }
        binding.tabRation.setOnClickListener  { loadFragment(RationStudioFragment()); setActiveTab(2) }
        binding.tabStock.setOnClickListener   { loadFragment(StockFragment());         setActiveTab(3) }
        binding.tabReport.setOnClickListener  { loadFragment(ReportFragment());        setActiveTab(4) }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun loadFragmentBackStack(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null).commit()
    }

    private fun setActiveTab(index: Int) {
        val tabs = listOf(
            binding.tabHome, binding.tabFeed,
            binding.tabRation, binding.tabStock, binding.tabReport
        )
        tabs.forEachIndexed { i, tab ->
            val label = tab.getChildAt(1) as? TextView ?: return@forEachIndexed
            label.setTextColor(
                if (i == index) Color.parseColor("#14B8A6") else Color.parseColor("#475569")
            )
        }
    }
}