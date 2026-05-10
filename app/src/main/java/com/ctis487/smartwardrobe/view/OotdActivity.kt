package com.ctis487.smartwardrobe.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityOotdBinding
import com.ctis487.smartwardrobe.network.*
import com.ctis487.smartwardrobe.utils.SoundHelper
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

class OotdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOotdBinding
    private val BASE_URL = "http://10.0.2.2:3001"

    companion object {
        private const val REQ_LOCATION = 101
        private var cachedOutfit: OutfitResult? = null
        private var cachedWeather: WeatherResponse? = null
        private var lastGeneratedDate: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOotdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        binding.btnGenerateOotd.setOnClickListener {
            startOotdFlow(forceRegenerate = true)
        }

        binding.btnVisualize.setOnClickListener {
            visualizeCurrentOutfit()
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (cachedOutfit != null && lastGeneratedDate == today) {
            renderOotd(cachedOutfit!!, cachedWeather)
        } else {
            startOotdFlow(forceRegenerate = false)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_ootd
    }

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

    private fun startOotdFlow(forceRegenerate: Boolean) {
        showLoading("Curating your look...")
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

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndGenerateOotd() {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Try to get location with a hard timeout of 1.5 seconds
            val location: Location? = withTimeoutOrNull(1500) {
                try {
                    val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } catch (e: Exception) { null }
            }

            // 2. Process whatever we have (even if null)
            processLocation(location)
        }
    }

    private suspend fun processLocation(location: Location?) {
        // Fallback to default (Ankara) if location is null
        val lat = location?.latitude ?: 39.9334 
        val lon = location?.longitude ?: 32.8597
        
        Log.d("OOTD", "Proceeding with location: $lat, $lon")
        
        try {
            // Call backend for weather - this will also return the City name
            val weatherRes = RetrofitClient.instance.getWeather(lat, lon).execute()
            if (weatherRes.isSuccessful && weatherRes.body()?.success == true) {
                val w = weatherRes.body()!!
                cachedWeather = w
                generateOotdWithWeather(w)
            } else {
                Log.e("OOTD", "Weather API failed or returned success=false")
                generateOotdWithWeather(null, lat, lon)
            }
        } catch (e: Exception) {
            Log.e("OOTD", "Weather call error: ${e.message}")
            generateOotdWithWeather(null, lat, lon)
        }
    }

    private fun generateOotdWithWeather(w: WeatherResponse?, lat: Double? = null, lon: Double? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val city = w?.city ?: "Current Location"
                
                withContext(Dispatchers.Main) { 
                    binding.tvWeather.text = if (w != null) "${w.icon} ${w.temp}°C" else "🌡️ --°C"
                    binding.tvEvents.text = "📍 $city"
                    showLoading("AI is styling your look for $city...") 
                }

                val weatherCtx = if (w != null) WeatherContext(
                    temp = w.temp,
                    conditionText = w.conditionText,
                    city = w.city,
                    icon = w.icon
                ) else null

                val query = "curate a perfect outfit for today ($today) in $city based on weather."

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
                            cachedOutfit = outfit
                            lastGeneratedDate = today
                            renderOotd(outfit, w)
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

    private fun renderOotd(outfit: OutfitResult, weather: WeatherResponse?) {
        binding.tvOotdBadge.text = "Stylist's Pick"
        val name = outfit.outfitName ?: outfit.name ?: "Your Daily Ensemble"
        binding.tvOotdTitle.text = name
        binding.tvReasoning.text = outfit.reasoning ?: "Curated just for you."

        if (weather != null) {
            binding.tvWeather.text = "${weather.icon} ${weather.temp}°C"
            binding.tvEvents.text = "📍 ${weather.city}"
        }

        binding.layoutItems.removeAllViews()
        outfit.items?.forEach { item ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_ootd_piece, binding.layoutItems, false)
            val img = view.findViewById<ImageView>(R.id.imgItem)
            val tvRole = view.findViewById<TextView>(R.id.tvItemRole)
            val tvName = view.findViewById<TextView>(R.id.tvItemName)
            val tvWorn = view.findViewById<TextView>(R.id.tvWornCount)
            val btnWorn = view.findViewById<ImageButton>(R.id.btnWorn)

            tvRole.text = (item.role ?: item.subcategory ?: "").uppercase()
            tvName.text = item.subcategory ?: ""
            tvWorn.text = "${item.wornCount ?: 0}w"
            btnWorn.setImageResource(R.drawable.ic_closet)

            btnWorn.setOnClickListener {
                markAsWorn(item.id ?: return@setOnClickListener, tvWorn)
            }

            if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load("$BASE_URL${item.imageUrl}")
                    .placeholder(R.color.card_bg)
                    .centerCrop()
                    .into(img)
            }
            binding.layoutItems.addView(view)
        }

        binding.btnVisualize.visibility = View.VISIBLE
        binding.layoutLoading.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
    }

    private fun visualizeCurrentOutfit() {
        val outfit = cachedOutfit ?: return
        val itemIds = outfit.items?.mapNotNull { it.id } ?: return
        if (itemIds.isEmpty()) return

        showLoading("Creating visualization...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = mapOf("itemIds" to itemIds, "stylingMode" to "unisex")
                val response = RetrofitClient.instance.visualizeOutfit(req).execute()
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val imageUrl = json.getString("imageUrl")
                        showVisualizationResult("$BASE_URL$imageUrl")
                    } else {
                        Toast.makeText(this@OotdActivity, "Visualization failed", Toast.LENGTH_SHORT).show()
                    }
                    hideLoading()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OotdActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    hideLoading()
                }
            }
        }
    }

    private fun showVisualizationResult(url: String) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Outfit Visualization")
            .setPositiveButton("Awesome", null)
            .create()
            
        val view = layoutInflater.inflate(R.layout.dialog_visualization, null)
        val img = view.findViewById<ImageView>(R.id.imgVisualization)
        
        Glide.with(this).load(url).into(img)
        dialog.setView(view)
        dialog.show()
    }

    private fun markAsWorn(itemId: String, tvWorn: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val emptyBody = "{}".toRequestBody("application/json".toMediaTypeOrNull())
                val response = RetrofitClient.instance.markItemWorn(itemId, emptyBody).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val newCount = json.optInt("wornCount", 0)
                        tvWorn.text = "${newCount}w"
                        
                        SoundHelper.playSuccessSound(this@OotdActivity)
                        Toast.makeText(this@OotdActivity, "Marked as worn!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("OotdActivity", "Worn error", e)
            }
        }
    }

    private fun showLoading(message: String = "Curating your look...") {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.tvLoadingMsg.text = message
        binding.layoutContent.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.btnVisualize.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
        binding.btnVisualize.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
