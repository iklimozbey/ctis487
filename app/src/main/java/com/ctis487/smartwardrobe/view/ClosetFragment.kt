package com.ctis487.smartwardrobe.view

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.room.Room
import com.ctis487.smartwardrobe.adapter.WardrobeAdapter
import com.ctis487.smartwardrobe.databinding.FragmentClosetBinding
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.db.ClothingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ClosetFragment : Fragment() {

    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WardrobeAdapter
    private val items = mutableListOf<ClothingItem>()
    private val allItems = mutableListOf<ClothingItem>()

    private lateinit var db: AppDatabase

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uploadImage(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Init Room
        db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "wardrobe_db"
        ).build()

        adapter = WardrobeAdapter(
            items,
            onDeleteClick = { item -> deleteItem(item) },
            onLaundryClick = { item -> moveToLaundry(item) },
            onItemClick = { item -> openDetailPage(item) }
        )

        binding.recyclerViewCloset.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewCloset.adapter = adapter

        binding.fabAdd.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // ✅ initial load
        loadClosetItems()
    }

    // 🔥 IMPORTANT: reload when coming back from detail page
    override fun onResume() {
        super.onResume()
        loadClosetItems()
    }

    // 🔥 Centralized loader
    private fun loadClosetItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val dbItems = db.clothingDao().getClosetItems()

            requireActivity().runOnUiThread {
                items.clear()
                items.addAll(dbItems)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun uploadImage(uri: Uri) {

        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val file = File(requireContext().cacheDir, "temp_image.jpg")

        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

        val body = MultipartBody.Part.createFormData(
            "image",
            file.name,
            requestFile
        )

        val call = com.ctis487.smartwardrobe.network.RetrofitClient
            .instance
            .uploadImage(body)

        call.enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {

            override fun onResponse(
                call: retrofit2.Call<okhttp3.ResponseBody>,
                response: retrofit2.Response<okhttp3.ResponseBody>
            ) {
                if (response.isSuccessful) {

                    val result = response.body()?.string()
                    Log.d("UPLOAD", "SUCCESS: $result")

                    val jsonObject = org.json.JSONObject(result)
                    val itemObj = jsonObject.getJSONObject("item")

                    val rawUrl = itemObj.optString("imageUrl", "")

                    if (rawUrl.isEmpty()) {
                        Log.e("UPLOAD", "NO IMAGE URL → skipping item")
                        return
                    }

                    val fixedUrl = "http://10.0.2.2:3001$rawUrl"

                    val subcategory = itemObj.optString("subcategory")
                        .takeIf { it.isNotBlank() && it != "null" } ?: "Unknown"

                    val color = itemObj.optString("primaryColor")
                        .takeIf { it.isNotBlank() && it != "null" } ?: "Unknown"

                    val newItem = ClothingItem(
                        id = itemObj.optString("id"),
                        imageUrl = fixedUrl,
                        subcategory = subcategory,
                        color = color,
                        status = "closet"
                    )

                    // ✅ SAVE TO ROOM
                    CoroutineScope(Dispatchers.IO).launch {
                        db.clothingDao().insertItem(newItem)
                        Log.d("ROOM_SAVE", "Saved to DB: $newItem")
                    }

                    // ✅ UPDATE UI instantly
                    requireActivity().runOnUiThread {
                        items.add(0, newItem)
                        adapter.notifyItemInserted(0)
                    }

                } else {
                    Log.e("UPLOAD", "ERROR: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(
                call: retrofit2.Call<okhttp3.ResponseBody>,
                t: Throwable
            ) {
                Log.e("UPLOAD", "FAIL: ${t.message}")
            }
        })
    }

    private fun deleteItem(item: ClothingItem) {
        CoroutineScope(Dispatchers.IO).launch {
            db.clothingDao().deleteItem(item)
        }

        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
    }

    private fun moveToLaundry(item: ClothingItem) {

        val updatedItem = item.copy(status = "laundry")

        CoroutineScope(Dispatchers.IO).launch {
            db.clothingDao().updateItem(updatedItem)
        }

        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
    }

    private fun openDetailPage(item: ClothingItem) {
        val intent = android.content.Intent(requireContext(), ItemDetailActivity::class.java)

        intent.putExtra("id", item.id)
        intent.putExtra("imageUrl", item.imageUrl)
        intent.putExtra("subcategory", item.subcategory)
        intent.putExtra("color", item.color)
        intent.putExtra("status", item.status)

        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}