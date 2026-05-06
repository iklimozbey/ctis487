package com.ctis487.smartwardrobe.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityProfileBinding
import com.ctis487.smartwardrobe.db.AppDatabase
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        setupBottomNavigation()
        loadChartData()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_closet -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    false
                }
                R.id.nav_ootd -> {
                    startActivity(Intent(this, OotdActivity::class.java))
                    false
                }
                R.id.nav_laundry -> {
                    startActivity(Intent(this, LaundryActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun loadChartData() {
        CoroutineScope(Dispatchers.IO).launch {
            val allItems = db.clothingDao().getAllItems()
            val categoryCounts = allItems.groupingBy { it.subcategory }.eachCount()

            withContext(Dispatchers.Main) {
                val entries = categoryCounts.map { PieEntry(it.value.toFloat(), it.key) }
                val dataSet = PieDataSet(entries, "Wardrobe Distribution")
                
                dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
                dataSet.valueTextColor = Color.WHITE
                dataSet.valueTextSize = 14f

                val pieData = PieData(dataSet)
                binding.pieChart.data = pieData
                binding.pieChart.description.isEnabled = false
                binding.pieChart.centerText = "Wardrobe"
                binding.pieChart.setCenterTextColor(Color.WHITE)
                binding.pieChart.setHoleColor(Color.TRANSPARENT)
                binding.pieChart.legend.textColor = Color.WHITE
                binding.pieChart.animateY(1000)
                binding.pieChart.invalidate()
            }
        }
    }
}
