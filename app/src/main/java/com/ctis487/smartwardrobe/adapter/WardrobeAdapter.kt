package com.ctis487.smartwardrobe.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import java.net.URL
import com.ctis487.smartwardrobe.databinding.ItemClosetBinding
import com.ctis487.smartwardrobe.databinding.ItemLaundryBinding
import com.ctis487.smartwardrobe.db.ClothingItem


class WardrobeAdapter(
    private var items: List<ClothingItem>,
    private val onDeleteClick: (ClothingItem) -> Unit,
    private val onLaundryClick: (ClothingItem) -> Unit,
    private val onWornClick: (ClothingItem) -> Unit,
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
            holder.binding.tvCategory.text = item.subcategory ?: "Unknown"
            holder.binding.tvColor.text = item.color ?: "N/A"
            holder.binding.tvTag.text = item.subcategory?.split(" ")?.last()?.uppercase() ?: "ITEM"
            holder.binding.tvWornCount.text = "${item.wornCount} wears"

            // Handle Premium Status Badges
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val isWornToday = item.lastWorn?.startsWith(today) == true
            
            when {
                isWornToday -> {
                    holder.binding.statusBadgeContainer.visibility = android.view.View.VISIBLE
                    holder.binding.tvStatusBadge.text = "WORN TODAY"
                    holder.binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(com.ctis487.smartwardrobe.R.color.accent))
                }
                item.status == "laundry" -> {
                    holder.binding.statusBadgeContainer.visibility = android.view.View.VISIBLE
                    holder.binding.tvStatusBadge.text = "IN LAUNDRY"
                    holder.binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(com.ctis487.smartwardrobe.R.color.red))
                }
                item.status == "winter-store" -> {
                    holder.binding.statusBadgeContainer.visibility = android.view.View.VISIBLE
                    holder.binding.tvStatusBadge.text = "❄️ WINTER"
                    holder.binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_light))
                }
                item.status == "summer-store" -> {
                    holder.binding.statusBadgeContainer.visibility = android.view.View.VISIBLE
                    holder.binding.tvStatusBadge.text = "☀️ SUMMER"
                    holder.binding.tvStatusBadge.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_light))
                }
                else -> {
                    holder.binding.statusBadgeContainer.visibility = android.view.View.GONE
                }
            }

            // ✅ Native Image loading without Glide
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val urlString = if (item.imageUrl.startsWith("http")) item.imageUrl else "http://10.0.2.2:3001${if (item.imageUrl.startsWith("/")) "" else "/"}${item.imageUrl}"
                    val stream = URL(urlString).openStream()
                    val bitmap = BitmapFactory.decodeStream(stream)
                    withContext(Dispatchers.Main) {
                        holder.binding.imgItem.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            holder.binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }

            holder.binding.btnLaundry.setOnClickListener {
                onLaundryClick(item)
            }

            holder.binding.btnWorn.setOnClickListener {
                onWornClick(item)
            }

            holder.binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
        else if (holder is LaundryViewHolder) {
            holder.binding.tvCategory.text = item.subcategory ?: "Unknown"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val urlString = if (item.imageUrl.startsWith("http")) item.imageUrl else "http://10.0.2.2:3001${if (item.imageUrl.startsWith("/")) "" else "/"}${item.imageUrl}"
                    val stream = URL(urlString).openStream()
                    val bitmap = BitmapFactory.decodeStream(stream)
                    withContext(Dispatchers.Main) {
                        holder.binding.imgItem.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

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