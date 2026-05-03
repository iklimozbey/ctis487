package com.ctis487.smartwardrobe

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.ctis487.smartwardrobe.databinding.ActivityMainBinding
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.view.ClosetFragment
import com.ctis487.smartwardrobe.view.LaundryFragment
import com.ctis487.smartwardrobe.view.OotdFragment
import com.ctis487.smartwardrobe.view.OutfitAiFragment
import com.ctis487.smartwardrobe.view.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "wardrobe_db"
        ).build()

        val dao = db.clothingDao()
        setupNavigation()
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(ClosetFragment())
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_closet -> ClosetFragment()
                R.id.nav_ootd -> OotdFragment()
                R.id.nav_laundry -> LaundryFragment()
                R.id.nav_outfit_ai -> OutfitAiFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> ClosetFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}