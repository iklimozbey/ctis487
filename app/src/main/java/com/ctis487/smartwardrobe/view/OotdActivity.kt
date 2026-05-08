package com.ctis487.smartwardrobe.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityOotdBinding

class OotdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOotdBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOotdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        checkLocationPermission()

        binding.btnGenerateOotd.setOnClickListener {
            Toast.makeText(this, "AI is matching your clothes...", Toast.LENGTH_SHORT).show()
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

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            getLocation()
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun getLocation() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            location?.let {
                binding.tvWeather.text = "Fetching weather..."
                fetchWeather(it.latitude, it.longitude)
            }
        } catch (e: Exception) {
            binding.tvWeather.text = "Weather Unavailable"
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        com.ctis487.smartwardrobe.network.WeatherRetrofitClient.instance.getCurrentWeather(lat, lon)
            .enqueue(object : retrofit2.Callback<com.ctis487.smartwardrobe.network.WeatherResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.ctis487.smartwardrobe.network.WeatherResponse>,
                    response: retrofit2.Response<com.ctis487.smartwardrobe.network.WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val temp = response.body()?.currentWeather?.temperature
                        val isDay = response.body()?.currentWeather?.is_day
                        val icon = if (isDay == 1) "☀️" else "🌙"
                        if (temp != null) {
                            binding.tvWeather.text = "$icon $temp°C"
                        }
                    } else {
                        binding.tvWeather.text = "⚠️ API Error"
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.ctis487.smartwardrobe.network.WeatherResponse>, t: Throwable) {
                    binding.tvWeather.text = "⚠️ Net Error"
                }
            })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        }
    }
}
