package com.ctis487.smartwardrobe.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ctis487.smartwardrobe.databinding.ItemClosetBinding
import com.ctis487.smartwardrobe.databinding.ItemLaundryBinding
import com.ctis487.smartwardrobe.model.ClothingItem

class WardrobeAdapter(
    private var items: List<ClothingItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_CLOSET = 1
        const val VIEW_TYPE_LAUNDRY = 2
    }

    class ClosetViewHolder(val binding: ItemClosetBinding) : RecyclerView.ViewHolder(binding.root)
    class LaundryViewHolder(val binding: ItemLaundryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (items[position].status == "closet") VIEW_TYPE_CLOSET else VIEW_TYPE_LAUNDRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_CLOSET) {
            ClosetViewHolder(ItemClosetBinding.inflate(inflater, parent, false))
        } else {
            LaundryViewHolder(ItemLaundryBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        // We leave actual content fetching here but simple assignments for now
        if (holder is ClosetViewHolder) {
            holder.binding.tvCategory.text = item.subcategory
            // Using placeholder logic or Coil if external library used
            // holder.binding.imgItem.load(item.imageUrl)
        } else if (holder is LaundryViewHolder) {
            holder.binding.tvCategory.text = item.subcategory
        }
    }
    override fun getItemCount() = items.size
    
    fun updateItems(newItems: List<ClothingItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
