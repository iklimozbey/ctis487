package com.ctis487.smartwardrobe.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.ctis487.smartwardrobe.adapter.WardrobeAdapter
import com.ctis487.smartwardrobe.databinding.FragmentLaundryBinding
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.db.ClothingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LaundryFragment : Fragment() {

    private var _binding: FragmentLaundryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WardrobeAdapter
    private val items = mutableListOf<ClothingItem>()
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLaundryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnWashAll.setOnClickListener {
            washAllItems()
        }

        db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "wardrobe_db"
        ).build()

        adapter = WardrobeAdapter(
            items,
            onDeleteClick = { item ->
                deleteItem(item)
            },
            onLaundryClick = { item ->
                moveToCloset(item)
            },
            onItemClick = { item ->

            }
        )
        binding.recyclerViewLaundry.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLaundry.adapter = adapter

        CoroutineScope(Dispatchers.IO).launch {
            val laundryItems = db.clothingDao().getAllItems()
                .filter { it.status == "laundry" }

            requireActivity().runOnUiThread {
                items.clear()
                items.addAll(laundryItems)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun moveToCloset(item: ClothingItem) {

        val updatedItem = item.copy(status = "closet")

        // 🔥 Update in Room
        CoroutineScope(Dispatchers.IO).launch {
            db.clothingDao().updateItem(updatedItem)
        }

        // 🔥 Remove from Laundry UI
        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
    }

    private fun deleteItem(item: ClothingItem) {

        // 🔥 Delete from Room
        CoroutineScope(Dispatchers.IO).launch {
            db.clothingDao().deleteItem(item)
        }

        // 🔥 Remove from UI
        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
    }

    private fun washAllItems() {

        CoroutineScope(Dispatchers.IO).launch {

            // 🔥 Get all laundry items
            val laundryItems = db.clothingDao().getAllItems()
                .filter { it.status == "laundry" }

            laundryItems.forEach { item ->
                val updatedItem = item.copy(status = "closet")
                db.clothingDao().updateItem(updatedItem)
            }


            requireActivity().runOnUiThread {
                items.clear()
                adapter.notifyDataSetChanged()
            }
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
