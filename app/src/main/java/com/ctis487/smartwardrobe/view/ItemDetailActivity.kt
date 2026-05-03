package com.ctis487.smartwardrobe.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.bumptech.glide.Glide
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityItemDetailBinding
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.db.ClothingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ItemDetailActivity : AppCompatActivity() {
    lateinit var binding : ActivityItemDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityItemDetailBinding.inflate(layoutInflater)

        setContentView(binding.root)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        ).build()

        val id = intent.getStringExtra("id") ?: return
        val btnSave = findViewById<android.widget.Button>(R.id.btnSave)
        val imageUrl = intent.getStringExtra("imageUrl")

        val img = findViewById<android.widget.ImageView>(R.id.imgDetail)

        Glide.with(this)
            .load(imageUrl)
            .placeholder(android.R.color.darker_gray)
            .error(android.R.color.holo_red_dark)
            .into(img)

        val category = intent.getStringExtra("subcategory")
        val color = intent.getStringExtra("color")

        val etCategory = findViewById<android.widget.EditText>(R.id.etCategory)
        val etColor = findViewById<android.widget.EditText>(R.id.etColor)

        etCategory.setText(category ?: "")
        etColor.setText(color ?: "")



        btnSave.setOnClickListener {

            val updatedCategory = etCategory.text.toString()
            val updatedColor = etColor.text.toString()

            val imageUrl = intent.getStringExtra("imageUrl") ?: ""
            val status = intent.getStringExtra("status") ?: "closet"

            val updatedItem = ClothingItem(
                id = id,
                imageUrl = imageUrl,
                subcategory = updatedCategory,
                color = updatedColor,
                status = status
            )

            CoroutineScope(Dispatchers.IO).launch {
                db.clothingDao().updateItem(updatedItem)
            }

            finish()
        }
    }
}