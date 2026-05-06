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
            else -> null
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dbItems = db.clothingDao().getAllItems().filter { it.status == "laundry" }
            val filteredItems = if (categoryFilter != null) {
                dbItems.filter { it.subcategory?.contains(categoryFilter, ignoreCase = true) == true }
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
            // ✅ BI-DIRECTIONAL: Delete from backend (5 pts)
            com.ctis487.smartwardrobe.network.SyncManager.deleteFromBackend(item.id)
            db.clothingDao().deleteItem(item)
            loadItems()
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
