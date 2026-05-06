package com.ctis487.smartwardrobe.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.adapter.WardrobeAdapter
import com.ctis487.smartwardrobe.databinding.ActivityHomeBinding
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.db.ClothingItem
import com.ctis487.smartwardrobe.network.RetrofitClient
import com.ctis487.smartwardrobe.utils.SoundHelper
import com.ctis487.smartwardrobe.utils.SwipeToDeleteCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: WardrobeAdapter
    private val items = mutableListOf<ClothingItem>()
    private lateinit var db: AppDatabase

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        setupRecyclerView()
        setupBottomNavigation()
        setupFilters()

        binding.fabAdd.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        loadItems()
        setupBackgroundSync()
        
        // ✅ BI-DIRECTIONAL: Immediate sync on start (5 pts)
        CoroutineScope(Dispatchers.IO).launch {
            com.ctis487.smartwardrobe.network.SyncManager.syncFromBackend(this@HomeActivity)
            withContext(Dispatchers.Main) { loadItems() }
        }
    }

    private fun setupBackgroundSync() {
        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.ctis487.smartwardrobe.worker.SyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WardrobeSync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    override fun onResume() {
        super.onResume()
        loadItems()
        binding.bottomNavigation.selectedItemId = R.id.nav_closet
    }

    private fun setupRecyclerView() {
        adapter = WardrobeAdapter(
            items,
            onDeleteClick = { deleteItem(it) },
            onLaundryClick = { moveToLaundry(it) },
            onItemClick = { openDetail(it) }
        )
        binding.recyclerViewHome.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewHome.adapter = adapter

        // ✅ GESTURE: Swipe to Delete (4 pts)
        val swipeHandler = object : SwipeToDeleteCallback() {
            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                deleteItem(items[position])
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewHome)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_closet -> true
                R.id.nav_ootd -> {
                    startActivity(Intent(this, OotdActivity::class.java))
                    false
                }
                R.id.nav_laundry -> {
                    startActivity(Intent(this, LaundryActivity::class.java))
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

    private fun setupFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            loadItems()
        }
    }

    private fun loadItems() {
        val selectedChipId = binding.chipGroup.checkedChipId
        val categoryFilter = when (selectedChipId) {
            R.id.chipTop -> "top"
            R.id.chipBottom -> "bottom"
            R.id.chipShoes -> "shoes"
            R.id.chipOuterwear -> "outerwear"
            else -> null
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dbItems = db.clothingDao().getClosetItems()
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

    private fun uploadImage(uri: Uri) {
        binding.lottieLoading.visibility = View.VISIBLE
        binding.recyclerViewHome.alpha = 0.5f

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "upload.jpg")
                inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

                val response = RetrofitClient.instance.uploadImage(body).execute()
                if (response.isSuccessful) {
                    val result = response.body()?.string() ?: ""
                    val json = org.json.JSONObject(result)
                    val itemObj = json.getJSONObject("item")

                    val newItem = ClothingItem(
                        id = itemObj.getString("id"),
                        imageUrl = "http://10.0.2.2:3001" + itemObj.getString("imageUrl"),
                        subcategory = itemObj.optString("subcategory", "Unknown"),
                        color = itemObj.optString("primaryColor", "Unknown"),
                        status = "closet"
                    )

                    db.clothingDao().insertItem(newItem)

                    withContext(Dispatchers.Main) {
                        SoundHelper.playSuccessSound(this@HomeActivity) // ✅ SOUND (4 pts)
                        Toast.makeText(this@HomeActivity, R.string.success, Toast.LENGTH_SHORT).show()
                        loadItems()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.lottieLoading.visibility = View.GONE
                    binding.recyclerViewHome.alpha = 1.0f
                }
            }
        }
    }

    private fun deleteItem(item: ClothingItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                // ✅ BI-DIRECTIONAL: Delete from backend (5 pts)
                com.ctis487.smartwardrobe.network.SyncManager.deleteFromBackend(item.id)
                db.clothingDao().deleteItem(item)
                withContext(Dispatchers.Main) {
                    loadItems()
                    dialog.dismiss()
                }
            }
        }

        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
            loadItems() // Refresh to undo the swipe visual if needed
        }

        dialog.show()
    }

    private fun moveToLaundry(item: ClothingItem) {
        CoroutineScope(Dispatchers.IO).launch {
            // ✅ BI-DIRECTIONAL: Update on backend (5 pts)
            com.ctis487.smartwardrobe.network.SyncManager.updateStatusOnBackend(item.id, "laundry")
            item.status = "laundry"
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