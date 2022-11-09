package com.amaromerovic.happyplaces

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amaromerovic.happyplaces.databinding.ActivityDetailsHappyPlaceBinding
import com.amaromerovic.happyplaces.model.HappyPlaceModel
import com.amaromerovic.happyplaces.util.Constants

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

        val happyPlace = Constants.getSerializable(
            this@DetailsHappyPlaceActivity,
            Constants.OBJECT_KEY,
            HappyPlaceModel::class.java
        )

        binding.titleText.text = happyPlace.title
        binding.descriptionText.text = happyPlace.description
        binding.dateText.text = happyPlace.date
        binding.imageView.setImageURI(Uri.parse(happyPlace.imageUri))


        binding.viewOnMap.setOnClickListener {
            val intent = Intent(this@DetailsHappyPlaceActivity, MapsActivity::class.java)
            intent.putExtra(Constants.OBJECT_KEY, happyPlace)
            startActivity(intent)
        }


    }




}