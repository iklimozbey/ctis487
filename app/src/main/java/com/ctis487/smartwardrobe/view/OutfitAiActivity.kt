package com.ctis487.smartwardrobe.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.adapter.WardrobeAdapter
import com.ctis487.smartwardrobe.databinding.ActivityOutfitAiBinding
import com.ctis487.smartwardrobe.db.ClothingItem
import com.ctis487.smartwardrobe.network.OutfitSearchRequest
import com.ctis487.smartwardrobe.network.OutfitSearchResponse
import com.ctis487.smartwardrobe.network.RetrofitClient
import com.ctis487.smartwardrobe.utils.SoundHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OutfitAiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOutfitAiBinding
    private lateinit var adapter: WardrobeAdapter
    private var currentItemIds: List<String> = emptyList()
    private val BASE_URL = "http://10.0.2.2:3001"

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

        binding.btnVisualize.setOnClickListener {
            visualizeOutfit()
        }
    }

    private fun setupRecyclerView() {
        adapter = WardrobeAdapter(
            emptyList(),
            onDeleteClick = {}, 
            onLaundryClick = {}, 
            onWornClick = { item -> markAsWorn(item) },
            onItemClick = { item -> openDetail(item) },
            isAiView = true
        )
        binding.recyclerViewAiItems.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewAiItems.adapter = adapter
    }

    private fun generateOutfit(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvOutfitName.visibility = View.GONE
        binding.btnVisualize.visibility = View.GONE
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
                        binding.tvOutfitName.text = outfit.name ?: outfit.outfitName
                        binding.tvOutfitName.visibility = View.VISIBLE
                        binding.btnVisualize.visibility = View.VISIBLE

                        currentItemIds = outfit.items?.mapNotNull { it.id } ?: emptyList()

                        val clothingItems = outfit.items?.map { item ->
                            ClothingItem(
                                id = item.id ?: "",
                                imageUrl = item.imageUrl ?: "",
                                category = null,
                                subcategory = item.subcategory,
                                color = item.primaryColor,
                                wornCount = item.wornCount ?: 0,
                                status = "closet"
                            )
                        } ?: emptyList()
                        adapter.updateItems(clothingItems)
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

    private fun visualizeOutfit() {
        if (currentItemIds.isEmpty()) return
        
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = mapOf("itemIds" to currentItemIds, "stylingMode" to "unisex")
                val response = RetrofitClient.instance.visualizeOutfit(req).execute()
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val body = response.body()?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val imageUrl = json.getString("imageUrl")
                        showVisualizationResult("$BASE_URL$imageUrl")
                    } else {
                        Toast.makeText(this@OutfitAiActivity, "Visualization failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@OutfitAiActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun markAsWorn(item: ClothingItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val emptyBody = "{}".toRequestBody("application/json".toMediaTypeOrNull())
                val response = RetrofitClient.instance.markItemWorn(item.id, emptyBody).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        SoundHelper.playSuccessSound(this@OutfitAiActivity)
                        Toast.makeText(this@OutfitAiActivity, "Marked as worn!", Toast.LENGTH_SHORT).show()
                        // Refresh worn count
                        val body = response.body()?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        item.wornCount = json.optInt("wornCount", item.wornCount)
                        adapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.e("OutfitAiActivity", "Worn error", e)
            }
        }
    }

    private fun openDetail(item: ClothingItem) {
        val intent = Intent(this, ItemDetailActivity::class.java)
        intent.putExtra("id", item.id)
        startActivity(intent)
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
                R.id.nav_outfit_ai -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
}
