package com.ctis487.smartwardrobe.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import android.graphics.BitmapFactory
import java.net.URL
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
        val db = AppDatabase.getDatabase(this)

        val id = intent.getStringExtra("id") ?: return
        val btnSave = findViewById<android.widget.Button>(R.id.btnSave)
        val imageUrl = intent.getStringExtra("imageUrl")

        val img = findViewById<android.widget.ImageView>(R.id.imgDetail)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!imageUrl.isNullOrEmpty()) {
                    val stream = URL(imageUrl).openStream()
                    val bitmap = BitmapFactory.decodeStream(stream)
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        img.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val category = intent.getStringExtra("subcategory")
        val color = intent.getStringExtra("color")

        val etCategory = findViewById<android.widget.EditText>(R.id.etCategory)
        val etColor = findViewById<android.widget.EditText>(R.id.etColor)

        etCategory.setText(category ?: "")
        etColor.setText(color ?: "")
        
        val colorCircle = findViewById<com.ctis487.smartwardrobe.customview.CircularColorView>(R.id.colorCircle)
        colorCircle.setColor(color ?: "#CCCCCC")

        etColor.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                colorCircle.setColor(s.toString())
            }
        })

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