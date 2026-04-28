package com.ctis487.smartwardrobe.view

import android.os.Bundle
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ctis487.smartwardrobe.adapter.WardrobeAdapter
import com.ctis487.smartwardrobe.databinding.FragmentClosetBinding
import com.ctis487.smartwardrobe.model.ClothingItem

class ClosetFragment : Fragment() {

    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WardrobeAdapter
    private val items = mutableListOf<ClothingItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClosetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDummyData()
        adapter = WardrobeAdapter(items)
        binding.recyclerViewCloset.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewCloset.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Feature logic for moving items to laundry
                val position = viewHolder.adapterPosition
                items[position].status = "laundry"
                
                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    RingtoneManager.getRingtone(requireContext(), uri).play()
                } catch (e: Exception) {}
                
                items.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewCloset)
    }

    private fun setupDummyData() {
        items.clear()
        items.add(ClothingItem("1", "", "Red T-Shirt", "Red", "closet"))
        items.add(ClothingItem("2", "", "Blue Jeans", "Blue", "closet"))
        items.add(ClothingItem("4", "", "Sneakers", "White", "closet"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
