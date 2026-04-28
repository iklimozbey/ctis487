package com.ctis487.smartwardrobe.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ctis487.smartwardrobe.adapter.WardrobeAdapter
import com.ctis487.smartwardrobe.databinding.FragmentLaundryBinding
import com.ctis487.smartwardrobe.model.ClothingItem

class LaundryFragment : Fragment() {

    private var _binding: FragmentLaundryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WardrobeAdapter
    private val items = mutableListOf<ClothingItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLaundryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDummyData()
        adapter = WardrobeAdapter(items)
        binding.recyclerViewLaundry.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewLaundry.adapter = adapter
    }

    private fun setupDummyData() {
        items.clear()
        items.add(ClothingItem("3", "", "Winter Coat", "Black", "laundry"))
        items.add(ClothingItem("5", "", "Formal Shirt", "White", "laundry"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
