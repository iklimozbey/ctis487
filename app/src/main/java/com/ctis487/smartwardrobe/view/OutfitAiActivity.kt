package com.ctis487.smartwardrobe.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityOutfitAiBinding

class OutfitAiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOutfitAiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOutfitAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_outfit_ai
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_closet -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    false
                }
                R.id.nav_ootd -> {
                    startActivity(Intent(this, OotdActivity::class.java))
                    finish()
                    false
                }
                R.id.nav_laundry -> {
                    startActivity(Intent(this, LaundryActivity::class.java))
                    finish()
                    false
                }
                R.id.nav_outfit_ai -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    false
                }
                else -> false
            }
        }
    }
}
