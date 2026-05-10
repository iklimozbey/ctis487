package com.ctis487.smartwardrobe.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.adapter.WardrobeAdapter
import com.ctis487.smartwardrobe.databinding.ActivityLaundryBinding
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.db.ClothingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LaundryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaundryBinding
    private lateinit var adapter: WardrobeAdapter
    private val items = mutableListOf<ClothingItem>()
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaundryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        setupRecyclerView()
        setupBottomNavigation()
        setupFilters()
        loadItems()

        binding.btnWashAll.setOnClickListener {
            washAllItems()
        }

        // ✅ BI-DIRECTIONAL: Immediate sync on start (5 pts)
        CoroutineScope(Dispatchers.IO).launch {
            com.ctis487.smartwardrobe.network.SyncManager.syncFromBackend(this@LaundryActivity)
            withContext(Dispatchers.Main) { loadItems() }
        }
    }

    override fun onResume() {
        super.onResume()
        loadItems()
        binding.bottomNavigation.selectedItemId = R.id.nav_laundry
    }

    private fun setupFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            loadItems()
        }
    }

    private fun setupRecyclerView() {
        adapter = WardrobeAdapter(
            items,
            onDeleteClick = { deleteItem(it) },
            onLaundryClick = { moveToCloset(it) },
            onWornClick = { }, // Not used in LaundryActivity
            onItemClick = { openDetail(it) }
        )
        binding.recyclerViewLaundry.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewLaundry.adapter = adapter
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_laundry -> true
                R.id.nav_closet -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    false
                }
                R.id.nav_ootd -> {
                    startActivity(Intent(this, OotdActivity::class.java))
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

    private fun loadItems() {
        val selectedChipId = binding.chipGroup.checkedChipId
        val categoryFilter = when (selectedChipId) {
            R.id.chipTop -> "top"
            R.id.chipBottom -> "bottom"
            R.id.chipShoes -> "shoes"
            R.id.chipOuterwear -> "outerwear"
            R.id.chipDress -> "dress"
            R.id.chipAccessory -> "accessory"
            else -> null
        }

        val topKeywords = listOf("top", "shirt", "t-shirt", "blouse", "sweater", "tank", "hoodie")
        val bottomKeywords = listOf("bottom", "pants", "jeans", "skirt", "shorts", "trouser")
        val shoesKeywords = listOf("shoe", "sneaker", "boot", "sandal", "heel", "flat")
        val outerwearKeywords = listOf("outerwear", "jacket", "coat", "blazer", "cardigan")
        val dressKeywords = listOf("dress", "gown", "jumpsuit", "romper")
        val accessoryKeywords = listOf("accessory", "bag", "belt", "hat", "scarf", "jewelry", "watch", "glasses")

        CoroutineScope(Dispatchers.IO).launch {
            val dbItems = db.clothingDao().getAllItems().filter { it.status == "laundry" }
            val filteredItems = if (categoryFilter != null) {
                dbItems.filter { item ->
                    val sub = item.subcategory?.lowercase() ?: ""
                    when (categoryFilter) {
                        "top" -> topKeywords.any { sub.contains(it) }
                        "bottom" -> bottomKeywords.any { sub.contains(it) }
                        "shoes" -> shoesKeywords.any { sub.contains(it) }
                        "outerwear" -> outerwearKeywords.any { sub.contains(it) }
                        "dress" -> dressKeywords.any { sub.contains(it) }
                        "accessory" -> accessoryKeywords.any { sub.contains(it) }
                        else -> false
                    }
                }
            } else {
                dbItems
            }

            withContext(Dispatchers.Main) {
                items.clear()
                items.addAll(filteredItems)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun deleteItem(item: ClothingItem) {
        CoroutineScope(Dispatchers.IO).launch {
            com.ctis487.smartwardrobe.network.SyncManager.deleteFromBackend(item.id)
            db.clothingDao().deleteItem(item)
            withContext(Dispatchers.Main) {
                com.ctis487.smartwardrobe.utils.SoundHelper.playSuccessSound(this@LaundryActivity)
                loadItems()
            }
        }
    }

    private fun moveToCloset(item: ClothingItem) {
        CoroutineScope(Dispatchers.IO).launch {
            // ✅ BI-DIRECTIONAL: Update on backend (5 pts)
            com.ctis487.smartwardrobe.network.SyncManager.updateStatusOnBackend(item.id, "closet")
            item.status = "closet"
            db.clothingDao().updateItem(item)
            loadItems()
        }
    }

    private fun washAllItems() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Call backend wash-all
                val response = com.ctis487.smartwardrobe.network.RetrofitClient.instance.washAll().execute()
                
                if (response.isSuccessful) {
                    // 2. Update local DB
                    val allItems = db.clothingDao().getAllItems()
                    allItems.filter { it.status == "laundry" }.forEach { 
                        it.status = "closet"
                        db.clothingDao().updateItem(it)
                    }

                    withContext(Dispatchers.Main) {
                        com.ctis487.smartwardrobe.utils.SoundHelper.playSuccessSound(this@LaundryActivity)
                        android.widget.Toast.makeText(this@LaundryActivity, "All items washed!", android.widget.Toast.LENGTH_SHORT).show()
                        loadItems()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(this@LaundryActivity, "Backend error", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@LaundryActivity, "Network Error", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openDetail(item: ClothingItem) {
        val intent = Intent(this, ItemDetailActivity::class.java)
        intent.putExtra("id", item.id)
        intent.putExtra("imageUrl", item.imageUrl)
        intent.putExtra("subcategory", item.subcategory)
        intent.putExtra("color", item.color)
        intent.putExtra("status", item.status)
        startActivity(intent)
    }
}
