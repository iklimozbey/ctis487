package com.ctis487.smartwardrobe.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityItemDetailBinding
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.db.ClothingItem
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ItemDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityItemDetailBinding
    private var currentItem: ClothingItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra("id") ?: run {
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        loadItemDetails(id)

        binding.btnSave.setOnClickListener {
            finish() // Just closing for now as metadata is usually AI-generated and read-only in this premium view
        }
    }

    private fun loadItemDetails(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@ItemDetailActivity)
            val item = db.clothingDao().getItemById(id)
            currentItem = item

            withContext(Dispatchers.Main) {
                item?.let { populateUI(it) } ?: run {
                    Toast.makeText(this@ItemDetailActivity, "Item not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun populateUI(item: ClothingItem) {
        // Image with Glide
        Glide.with(this)
            .load(item.imageUrl)
            .placeholder(R.color.card_bg)
            .transform(CenterCrop())
            .into(binding.imgDetail)

        // Basic Info
        binding.tvCategoryName.text = item.category ?: "ITEM"
        binding.tvSubcategory.text = item.subcategory ?: "Unknown Item"
        binding.tvDescription.text = item.description ?: "No description available."

        // Attributes
        val colorText = if (!item.secondaryColor.isNullOrEmpty()) "${item.color} / ${item.secondaryColor}" else item.color
        setupAttributeRow(binding.rowColor.root, getString(R.string.color), colorText, true)
        setupAttributeRow(binding.rowTone.root, getString(R.string.tone), item.colorTone)
        setupAttributeRow(binding.rowStyle.root, getString(R.string.style), item.style)
        setupAttributeRow(binding.rowPattern.root, getString(R.string.pattern), item.pattern)
        setupAttributeRow(binding.rowMaterial.root, getString(R.string.material), item.material)
        setupAttributeRow(binding.rowFit.root, getString(R.string.fit), item.fit)
        
        val seasons = item.season?.joinToString(", ") ?: "—"
        setupAttributeRow(binding.rowSeason.root, getString(R.string.season), seasons)

        // Occasion Chips
        binding.chipGroupOccasions.removeAllViews()
        item.occasionTags?.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCheckable = false
                chipBackgroundColor = getColorStateList(R.color.card_bg)
                setTextColor(getColor(R.color.white))
                chipStrokeWidth = 2f
                chipStrokeColor = getColorStateList(R.color.border_color)
            }
            binding.chipGroupOccasions.addView(chip)
        }
    }

    private fun setupAttributeRow(row: View, label: String, value: String?, showColor: Boolean = false) {
        val tvLabel = row.findViewById<android.widget.TextView>(R.id.tvLabel)
        val tvValue = row.findViewById<android.widget.TextView>(R.id.tvValue)
        val colorCircle = row.findViewById<com.ctis487.smartwardrobe.customview.CircularColorView>(R.id.colorCircle)

        tvLabel.text = label
        tvValue.text = value ?: "—"
        
        if (showColor && !value.isNullOrEmpty()) {
            colorCircle.visibility = View.VISIBLE
            colorCircle.setColor(value)
        } else {
            colorCircle.visibility = View.GONE
        }
    }
}