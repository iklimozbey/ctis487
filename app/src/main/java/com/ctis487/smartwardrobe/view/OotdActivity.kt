package com.ctis487.smartwardrobe.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityOotdBinding
import com.ctis487.smartwardrobe.network.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class OotdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOotdBinding
    private val BASE_URL = "http://10.0.2.2:3001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOotdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        binding.btnGenerateOotd.setOnClickListener {
            startOotdFlow()
        }

        startOotdFlow()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_ootd
    }

    // ── Navigation ──────────────────────────────────────────────
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ootd -> true
                R.id.nav_closet -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    false
                }
                R.id.nav_laundry -> {
                    startActivity(Intent(this, LaundryActivity::class.java))
                    false
                }
                R.id.nav_outfit_ai -> {
                    startActivity(Intent(this, OutfitAiActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    // ── OOTD Entry ──────────────────────────────────────────────
    private fun startOotdFlow() {
        showLoading("Locating you...")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
        } else {
            fetchLocationAndGenerateOotd()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndGenerateOotd()
        } else {
            // Proceed without GPS — backend will use defaults (Ankara-ish)
            generateOotdWithCoords(null, null, "Your Area")
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndGenerateOotd() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location: Location? = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    withContext(Dispatchers.Main) { showLoading("Checking weather...") }

                    // Reverse geocode via backend proxy
                    var city = "Your Area"
                    try {
                        val geoRes = RetrofitClient.instance.reverseGeocode(lat, lon).execute()
                        if (geoRes.isSuccessful) city = geoRes.body()?.city ?: "Your Area"
                    } catch (_: Exception) {}

                    generateOotdWithCoords(lat, lon, city)
                } else {
                    generateOotdWithCoords(null, null, "Your Area")
                }
            } catch (e: Exception) {
                generateOotdWithCoords(null, null, "Your Area")
            }
        }
    }

    // ── Core OOTD Generation ─────────────────────────────────────
    private fun generateOotdWithCoords(lat: Double?, lon: Double?, city: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // 1. Fetch hourly weather
                withContext(Dispatchers.Main) { showLoading("Checking weather in $city...") }
                var weatherCtx: WeatherContext? = null
                var weatherDisplayText = "📍 $city"

                if (lat != null && lon != null) {
                    try {
                        val wRes = RetrofitClient.instance.getWeather(
                            lat = lat, lon = lon, city = city, date = today, hourly = true
                        ).execute()
                        if (wRes.isSuccessful) {
                            val w = wRes.body()
                            // Pick daytime hour (noon or current)
                            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                            val hourData = w?.hourly?.getOrNull(hour) ?: w?.hourly?.getOrNull(12)
                            val temp = hourData?.temp ?: w?.temp
                            val cond = hourData?.condition ?: w?.conditionText
                            val icon = hourData?.icon ?: w?.icon ?: "🌤️"

                            weatherCtx = WeatherContext(
                                temp = temp,
                                conditionText = cond,
                                city = city,
                                icon = icon
                            )
                            weatherDisplayText = "$icon ${temp}°C · ${cond ?: ""}"
                        }
                    } catch (_: Exception) {}
                }

                // 2. Build OOTD query (mirrors the frontend's exact prompt)
                withContext(Dispatchers.Main) { showLoading("AI is styling your look...") }
                val dateStr = today
                val query = "curate a perfect outfit for today ($dateStr) in $city based on weather."

                // 3. Call /api/search with weather context
                val searchRes = RetrofitClient.instance.searchOutfit(
                    OutfitSearchRequest(
                        query = query,
                        stylingMode = "unisex",
                        weather = weatherCtx
                    )
                ).execute()

                withContext(Dispatchers.Main) {
                    if (searchRes.isSuccessful && searchRes.body()?.success == true) {
                        val outfit = searchRes.body()?.outfit
                        if (outfit != null) {
                            renderOotd(outfit, weatherDisplayText, city)
                        } else {
                            showError("No outfit found. Add more clothes to your closet!")
                        }
                    } else {
                        showError("Couldn't generate outfit. Try again.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Error: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    // ── Rendering ────────────────────────────────────────────────
    private fun renderOotd(outfit: OutfitResult, weatherText: String, city: String) {
        // Update weather / badge pills
        binding.tvWeather.text = "🌡️ $weatherText"
        binding.tvEvents.text = "📍 $city"
        binding.tvOotdBadge.text = "Stylist's Pick"

        // Outfit name & reasoning
        val name = outfit.outfitName ?: outfit.name ?: "Your Daily Ensemble"
        binding.tvOotdTitle.text = name
        binding.tvReasoning.text = outfit.reasoning ?: "Curated just for you."

        // Populate item thumbnails
        binding.layoutItems.removeAllViews()
        outfit.items?.forEach { item ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_ootd_piece, binding.layoutItems, false)
            val img = view.findViewById<ImageView>(R.id.imgItem)
            val tvRole = view.findViewById<TextView>(R.id.tvItemRole)
            val tvName = view.findViewById<TextView>(R.id.tvItemName)

            tvRole.text = (item.role ?: item.subcategory ?: "").uppercase()
            tvName.text = item.subcategory ?: ""

            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load("$BASE_URL${item.imageUrl}")
                    .placeholder(R.color.card_bg)
                    .centerCrop()
                    .into(img)
            }
            binding.layoutItems.addView(view)
        }

        // Show content, hide loading
        binding.layoutLoading.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
    }

    private fun showLoading(message: String = "Curating your look...") {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.tvLoadingMsg.text = message
        binding.layoutContent.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQ_LOCATION = 101
    }
}
