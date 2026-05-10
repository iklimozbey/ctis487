package com.ctis487.smartwardrobe.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityAnalyticsBinding
import com.ctis487.smartwardrobe.network.AnalyticsData
import com.ctis487.smartwardrobe.network.AnalyticsResponse
import com.ctis487.smartwardrobe.network.RetrofitClient
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchAnalytics()

        binding.btnRefresh.setOnClickListener {
            fetchAnalytics()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun fetchAnalytics() {
        binding.lottieLoading.visibility = View.VISIBLE
        binding.analyticsContent.visibility = View.GONE

        RetrofitClient.instance.getAnalytics().enqueue(object : Callback<AnalyticsResponse> {
            override fun onResponse(call: Call<AnalyticsResponse>, response: Response<AnalyticsResponse>) {
                binding.lottieLoading.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    binding.analyticsContent.visibility = View.VISIBLE
                    response.body()?.analytics?.let { populateUI(it) }
                } else {
                    Toast.makeText(this@AnalyticsActivity, "Failed to load analytics", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AnalyticsResponse>, t: Throwable) {
                binding.lottieLoading.visibility = View.GONE
                Toast.makeText(this@AnalyticsActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateUI(data: AnalyticsData) {
        // Basic Stats
        binding.tvTotalItems.text = data.stats.totalItems.toString()
        binding.tvTotalWears.text = data.stats.totalWears.toString()

        // Color Pie Chart
        setupColorChart(data.colors.owned)

        // Category Bar Chart
        setupCategoryChart(data.categories.worn)

        // Top Worn Items
        binding.layoutTopWorn.removeAllViews()
        data.topWorn.forEach { item ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_analytics_row, binding.layoutTopWorn, false)
            itemView.findViewById<TextView>(R.id.tvTitle).text = item.name
            itemView.findViewById<TextView>(R.id.tvSubtitle).text = "${item.count} wears"
            val imageView = itemView.findViewById<android.widget.ImageView>(R.id.imgItem)
            
            val url = if (item.image?.startsWith("http") == true) item.image 
                      else "http://10.0.2.2:3001${if (item.image?.startsWith("/") == true) "" else "/"}${item.image}"
            
            Glide.with(this).load(url).into(imageView)
            binding.layoutTopWorn.addView(itemView)
        }

        // Dormant Items
        binding.layoutDormant.removeAllViews()
        data.dormant.forEach { item ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_analytics_row, binding.layoutDormant, false)
            itemView.findViewById<TextView>(R.id.tvTitle).text = item.name
            itemView.findViewById<TextView>(R.id.tvSubtitle).text = item.reason
            itemView.findViewById<android.widget.ImageView>(R.id.imgItem).setImageResource(R.drawable.ic_closet)
            binding.layoutDormant.addView(itemView)
        }
    }

    private fun setupColorChart(owned: Map<String, Int>) {
        val aggregated = mutableMapOf<String, Int>()
        val baseColors = listOf("blue", "red", "green", "yellow", "black", "white", "gray", "grey", "orange", "purple", "pink", "brown", "beige", "teal", "maroon", "silver", "gold")
        
        owned.forEach { (name, count) ->
            val lowerName = name.lowercase()
            val base = baseColors.find { lowerName.contains(it) } ?: lowerName
            val normalizedBase = if (base == "grey") "gray" else base
            aggregated[normalizedBase] = aggregated.getOrDefault(normalizedBase, 0) + count
        }

        val entries = aggregated.map { PieEntry(it.value.toFloat(), it.key.replaceFirstChar { it.uppercase() }) }
        val dataSet = PieDataSet(entries, "")
        
        // Semantic Color Mapping
        val colors = aggregated.keys.map { getColorForName(it) }
        dataSet.colors = colors
        
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f
        dataSet.sliceSpace = 3f
        
        // Use darker labels for very light colors
        dataSet.valueLineColor = Color.WHITE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        
        val pieData = PieData(dataSet)
        binding.chartColors.apply {
            data = pieData
            description.isEnabled = false
            legend.textColor = Color.WHITE
            legend.isWordWrapEnabled = true
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            setHoleColor(Color.TRANSPARENT)
            setCenterText("Owned\nColors")
            setCenterTextColor(Color.WHITE)
            setCenterTextSize(14f)
            animateXY(1200, 1200)
            invalidate()
        }
    }

    private fun getColorForName(name: String): Int {
        val lowerName = name.lowercase()
        return try {
            when {
                lowerName.contains("black") -> Color.BLACK
                lowerName.contains("white") -> Color.WHITE
                lowerName.contains("red") -> Color.RED
                lowerName.contains("blue") -> Color.BLUE
                lowerName.contains("navy") -> Color.parseColor("#000080")
                lowerName.contains("green") -> Color.parseColor("#4CAF50")
                lowerName.contains("yellow") -> Color.YELLOW
                lowerName.contains("gray") || lowerName.contains("grey") -> Color.GRAY
                lowerName.contains("orange") -> Color.parseColor("#FF9800")
                lowerName.contains("purple") -> Color.parseColor("#9C27B0")
                lowerName.contains("pink") -> Color.parseColor("#E91E63")
                lowerName.contains("brown") -> Color.parseColor("#795548")
                lowerName.contains("beige") -> Color.parseColor("#F5F5DC")
                lowerName.contains("teal") -> Color.parseColor("#008080")
                lowerName.contains("maroon") -> Color.parseColor("#800000")
                lowerName.contains("silver") -> Color.parseColor("#C0C0C0")
                lowerName.contains("gold") -> Color.parseColor("#FFD700")
                lowerName.startsWith("#") -> Color.parseColor(name)
                else -> ColorTemplate.MATERIAL_COLORS[0]
            }
        } catch (e: Exception) {
            Color.GRAY
        }
    }

    private fun setupCategoryChart(worn: Map<String, Int>) {
        val labels = worn.keys.toList()
        val entries = worn.values.mapIndexed { index, count -> BarEntry(index.toFloat(), count.toFloat()) }
        
        val dataSet = BarDataSet(entries, "Wears")
        dataSet.color = Color.parseColor("#C8A96E") // Accent color
        dataSet.valueTextColor = Color.WHITE
        
        val barData = BarData(dataSet)
        binding.chartCategories.data = barData
        binding.chartCategories.description.isEnabled = false
        
        val xAxis = binding.chartCategories.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        
        binding.chartCategories.axisLeft.textColor = Color.WHITE
        binding.chartCategories.axisRight.isEnabled = false
        binding.chartCategories.legend.isEnabled = false
        binding.chartCategories.animateY(1000)
        binding.chartCategories.invalidate()
    }
}
