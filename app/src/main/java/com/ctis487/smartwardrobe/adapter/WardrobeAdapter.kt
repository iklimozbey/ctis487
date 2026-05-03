package com.ctis487.smartwardrobe.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ctis487.smartwardrobe.databinding.ItemClosetBinding
import com.ctis487.smartwardrobe.databinding.ItemLaundryBinding
import com.ctis487.smartwardrobe.db.ClothingItem

class WardrobeAdapter(
    private var items: List<ClothingItem>,
    private val onDeleteClick: (ClothingItem) -> Unit,
    private val onLaundryClick: (ClothingItem) -> Unit,
    private val onItemClick: (ClothingItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_CLOSET = 1
        const val VIEW_TYPE_LAUNDRY = 2
    }

    class ClosetViewHolder(val binding: ItemClosetBinding) :
        RecyclerView.ViewHolder(binding.root)

    class LaundryViewHolder(val binding: ItemLaundryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (items[position].status == "closet")
            VIEW_TYPE_CLOSET
        else
            VIEW_TYPE_LAUNDRY
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

        if (holder is ClosetViewHolder) {
            holder.binding.tvCategory.text = item.subcategory

            Log.d("IMAGE_URL", item.imageUrl)

            val context = holder.itemView.context

            // ✅ Image loading
            Glide.with(context)
                .load(item.imageUrl)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .centerCrop()
                .into(holder.binding.imgItem)

            // 🔁 Retry
            holder.binding.imgItem.postDelayed({
                Glide.with(context)
                    .load(item.imageUrl)
                    .centerCrop()
                    .into(holder.binding.imgItem)
            }, 1200)



            holder.binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }

            holder.binding.btnLaundry.setOnClickListener {
                onLaundryClick(item)
            }
            holder.binding.root.setOnClickListener {
                onItemClick(item)
            }

        }
        else if (holder is LaundryViewHolder) {
            holder.binding.tvCategory.text = item.subcategory

            val context = holder.itemView.context

            Glide.with(context)
                .load(item.imageUrl)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .centerCrop()
                .into(holder.binding.imgItem)


            holder.binding.btnLaundry.setOnClickListener {
                onLaundryClick(item)
            }

            holder.binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ClothingItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}