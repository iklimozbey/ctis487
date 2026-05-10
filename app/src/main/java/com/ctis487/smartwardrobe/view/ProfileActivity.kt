package com.ctis487.smartwardrobe.view

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ctis487.smartwardrobe.R
import com.ctis487.smartwardrobe.databinding.ActivityProfileBinding
import com.ctis487.smartwardrobe.network.RetrofitClient
import com.ctis487.smartwardrobe.network.UserProfile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var currentProfile = UserProfile()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupCardSelections()
        setupTimePicker()
        setupBottomNavigation()
        fetchProfile()

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
    }

    private fun setupSpinners() {
        // Labels (What the user sees)
        val professions = resources.getStringArray(R.array.professions)
        val ageGroups = resources.getStringArray(R.array.age_groups)
        val ootdModes = resources.getStringArray(R.array.ootd_modes)
        val skinTones = resources.getStringArray(R.array.skin_tones)
        val styleAesthetics = resources.getStringArray(R.array.style_aesthetics)

        binding.spinnerProfession.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, professions)
        binding.spinnerAgeGroup.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ageGroups)
        binding.spinnerOotdMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ootdModes)
        binding.spinnerSkinTone.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, skinTones)
        binding.spinnerStylePref.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, styleAesthetics)
    }

    private fun setupCardSelections() {
        binding.cardMan.setOnClickListener { selectGender("man") }
        binding.cardWoman.setOnClickListener { selectGender("woman") }
        binding.cardUnisex.setOnClickListener { selectGender("unisex") }

        binding.cardRectangle.setOnClickListener { selectBodyType("rectangle") }
        binding.cardPear.setOnClickListener { selectBodyType("pear") }
        binding.cardInvTriangle.setOnClickListener { selectBodyType("inverted-triangle") }
        binding.cardHourglass.setOnClickListener { selectBodyType("hourglass") }
        binding.cardApple.setOnClickListener { selectBodyType("apple") }
    }

    private fun selectGender(gender: String) {
        currentProfile.gender = gender
        binding.cardMan.isSelected = gender == "man"
        binding.cardWoman.isSelected = gender == "woman"
        binding.cardUnisex.isSelected = gender == "unisex"
    }

    private fun selectBodyType(bodyType: String) {
        currentProfile.bodyType = bodyType
        binding.cardRectangle.isSelected = bodyType == "rectangle"
        binding.cardPear.isSelected = bodyType == "pear"
        binding.cardInvTriangle.isSelected = bodyType == "inverted-triangle"
        binding.cardHourglass.isSelected = bodyType == "hourglass"
        binding.cardApple.isSelected = bodyType == "apple"
    }

    private fun setupTimePicker() {
        binding.etOotdTime.setOnClickListener {
            val parts = binding.etOotdTime.text.toString().split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            TimePickerDialog(this, { _, h, m ->
                binding.etOotdTime.setText(String.format("%02d:%02d", h, m))
            }, hour, minute, true).show()
        }
    }

    private fun fetchProfile() {
        RetrofitClient.instance.getProfile().enqueue(object : Callback<com.ctis487.smartwardrobe.network.ProfileResponse> {
            override fun onResponse(call: Call<com.ctis487.smartwardrobe.network.ProfileResponse>, response: Response<com.ctis487.smartwardrobe.network.ProfileResponse>) {
                if (response.isSuccessful) {
                    response.body()?.profile?.let {
                        currentProfile = it
                        populateUI(it)
                    }
                }
            }
            override fun onFailure(call: Call<com.ctis487.smartwardrobe.network.ProfileResponse>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, R.string.error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateUI(profile: UserProfile) {
        selectGender(profile.gender ?: "unisex")
        selectBodyType(profile.bodyType ?: "rectangle")

        // Map values (English) to indices, then set spinner selection (Labels)
        setSpinnerSelectionByValue(binding.spinnerProfession, profile.profession, R.array.professions_values)
        setSpinnerSelectionByValue(binding.spinnerAgeGroup, profile.ageGroup, R.array.age_groups_values)
        setSpinnerSelectionByValue(binding.spinnerOotdMode, profile.ootdCountMode, R.array.ootd_modes_values)
        setSpinnerSelectionByValue(binding.spinnerSkinTone, profile.skinTone, R.array.skin_tones_values)
        setSpinnerSelectionByValue(binding.spinnerStylePref, profile.stylePref, R.array.style_aesthetics_values)

        binding.etLocation.setText(profile.location)
        binding.etIcalUrl.setText(profile.icalUrl)
        binding.etOotdTime.setText(profile.ootdTime ?: "21:00")
        binding.etHeight.setText(profile.height?.toString())
        binding.etWeight.setText(profile.weight?.toString())
        binding.etBio.setText(profile.bio)
    }

    private fun setSpinnerSelectionByValue(spinner: Spinner, value: String?, valuesArrayRes: Int) {
        val values = resources.getStringArray(valuesArrayRes)
        val index = values.indexOfFirst { it.equals(value, ignoreCase = true) }
        if (index >= 0) spinner.setSelection(index)
    }

    private fun getSpinnerValueByIndex(spinner: Spinner, valuesArrayRes: Int): String {
        val values = resources.getStringArray(valuesArrayRes)
        val index = spinner.selectedItemPosition
        return if (index in values.indices) values[index] else ""
    }

    private fun saveProfile() {
        currentProfile.apply {
            // Get English keywords for backend even if UI is in Turkish
            profession = getSpinnerValueByIndex(binding.spinnerProfession, R.array.professions_values)
            ageGroup = getSpinnerValueByIndex(binding.spinnerAgeGroup, R.array.age_groups_values)
            ootdCountMode = getSpinnerValueByIndex(binding.spinnerOotdMode, R.array.ootd_modes_values)
            skinTone = getSpinnerValueByIndex(binding.spinnerSkinTone, R.array.skin_tones_values)
            stylePref = getSpinnerValueByIndex(binding.spinnerStylePref, R.array.style_aesthetics_values)
            
            location = binding.etLocation.text.toString()
            icalUrl = binding.etIcalUrl.text.toString()
            ootdTime = binding.etOotdTime.text.toString()
            height = binding.etHeight.text.toString().toIntOrNull() ?: 175
            weight = binding.etWeight.text.toString().toIntOrNull() ?: 70
            bio = binding.etBio.text.toString()
        }

        RetrofitClient.instance.saveProfile(currentProfile).enqueue(object : Callback<com.ctis487.smartwardrobe.network.ProfileResponse> {
            override fun onResponse(call: Call<com.ctis487.smartwardrobe.network.ProfileResponse>, response: Response<com.ctis487.smartwardrobe.network.ProfileResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, R.string.success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ProfileActivity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.ctis487.smartwardrobe.network.ProfileResponse>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, R.string.error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
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
                R.id.nav_outfit_ai -> {
                    startActivity(Intent(this, OutfitAiActivity::class.java))
                    finish()
                    false
                }
                else -> false
            }
        }
    }
}
