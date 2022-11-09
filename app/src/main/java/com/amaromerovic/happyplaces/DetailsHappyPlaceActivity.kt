package com.amaromerovic.happyplaces

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amaromerovic.happyplaces.databinding.ActivityDetailsHappyPlaceBinding

class DetailsHappyPlaceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsHappyPlaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarDetailPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        binding.toolbarDetailPlace.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val title = intent.getStringExtra("HappyPlaceTitleKey")
        val description = intent.getStringExtra("HappyPlaceDescriptionKey")
        val date = intent.getStringExtra("HappyPlaceDateKey")
        val imageUri = intent.getStringExtra("HappyPlaceImageKey")

        if (title!!.isNotEmpty() && description!!.isNotEmpty() && date!!.isNotEmpty() && imageUri!!.isNotEmpty()) {
            binding.titleText.text = title
            binding.descriptionText.text = description
            binding.dateText.text = date
            binding.imageView.setImageURI(Uri.parse(imageUri))
        }


    }
}