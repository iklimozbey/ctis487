package com.ctis487.smartwardrobe.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.adapter.WardrobeAdapter
import com.ctis487.smartwardrobe.databinding.ActivityOutfitAiBinding
import com.ctis487.smartwardrobe.network.OutfitSearchRequest
import com.ctis487.smartwardrobe.network.OutfitSearchResponse
import com.ctis487.smartwardrobe.network.RetrofitClient
import com.ctis487.smartwardrobe.utils.SoundHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OutfitAiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOutfitAiBinding
    private lateinit var adapter: WardrobeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutfitAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupRecyclerView()

        binding.btnGenerate.setOnClickListener {
            val query = binding.etSearchQuery.text.toString()
            if (query.isNotBlank()) {
                generateOutfit(query)
            } else {
                Toast.makeText(this, "Please enter a prompt first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = WardrobeAdapter(
            emptyList(),
            onDeleteClick = {}, // Disabled for AI view
            onLaundryClick = {}, // Disabled for AI view
            onItemClick = {}
        )
        binding.recyclerViewAiItems.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewAiItems.adapter = adapter
    }

    private fun generateOutfit(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvOutfitName.visibility = View.GONE
        binding.recyclerViewAiItems.alpha = 0.5f

        val prefs = getSharedPreferences("SmartWardrobePrefs", MODE_PRIVATE)
        val gender = prefs.getString("gender_preference", "Female")
        val finalQuery = "$query (Style preference: $gender)"

        val request = OutfitSearchRequest(finalQuery)
        RetrofitClient.instance.searchOutfit(request).enqueue(object : Callback<OutfitSearchResponse> {
            override fun onResponse(call: Call<OutfitSearchResponse>, response: Response<OutfitSearchResponse>) {
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewAiItems.alpha = 1.0f

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    val outfit = body?.outfit
                    
                    if (outfit != null) {
                        SoundHelper.playSuccessSound(this@OutfitAiActivity)
                        binding.tvOutfitName.text = outfit.name
                        
                        binding.tvOutfitName.visibility = View.VISIBLE
                        
                        adapter.updateItems(outfit.items)
                    } else {
                        Toast.makeText(this@OutfitAiActivity, body?.message ?: "No outfit found.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@OutfitAiActivity, "Failed to generate outfit.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OutfitSearchResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewAiItems.alpha = 1.0f
                Toast.makeText(this@OutfitAiActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_outfit_ai
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_closet -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    false
                }
                R.id.nav_ootd -> {
                    startActivity(Intent(this, OotdActivity::class.java))
                    finish()
                    false
                }
                R.id.nav_laundry -> {
                    startActivity(Intent(this, LaundryActivity::class.java))
                    finish()
                    false
                }
                R.id.nav_outfit_ai -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    false
                }
                else -> false
            }
        }
    }
}
