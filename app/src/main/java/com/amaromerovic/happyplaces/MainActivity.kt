package com.amaromerovic.happyplaces

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amaromerovic.happyplaces.adapter.HappyPlaceRecyclerViewAdapter
import com.amaromerovic.happyplaces.data.HappyPlaceApp
import com.amaromerovic.happyplaces.data.HappyPlaceDAO
import com.amaromerovic.happyplaces.databinding.ActivityMainBinding
import com.amaromerovic.happyplaces.model.HappyPlaceModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), HappyPlaceRecyclerViewAdapter.OnHappyPlaceClickListener {
    private lateinit var binding: ActivityMainBinding
    private var happyPlaceDAO: HappyPlaceDAO? = null
    private lateinit var happyPlaces: List<HappyPlaceModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        happyPlaceDAO = (application as HappyPlaceApp).database.happyPlaceDAO()

        lifecycleScope.launch {
            happyPlaceDAO?.getAllHappyPlaces()?.collect {
                happyPlaces = it
                val adapter = HappyPlaceRecyclerViewAdapter(it, this@MainActivity)
                binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                binding.recyclerView.adapter = adapter
            }
        }
        binding.addButton.setOnClickListener {
            val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
            startActivity(intent)
        }

        swipeToDeleteOrEdit()

    }

    override fun onHappyPlaceClicked(position: Int) {
        lifecycleScope.launch {
            val happyPlace = happyPlaces[position]
            val intent = Intent(this@MainActivity, DetailsHappyPlaceActivity::class.java)
            intent.putExtra("HappyPlaceTitleKey", happyPlace.title)
            intent.putExtra("HappyPlaceDescriptionKey", happyPlace.description)
            intent.putExtra("HappyPlaceDateKey", happyPlace.date)
            intent.putExtra("HappyPlaceImageKey", happyPlace.imageUri)
            startActivity(intent)
        }
    }

    private fun swipeToDeleteOrEdit() {
        val swipe: Swipe = object : Swipe(this@MainActivity) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
                val happyPlace = happyPlaces[viewHolder.adapterPosition]
                if (i >= 8) {
                    val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
                    intent.putExtra("HappyPlaceIdKey", happyPlace.id)
                    intent.putExtra("HappyPlaceTitleKey", happyPlace.title)
                    intent.putExtra("HappyPlaceDescriptionKey", happyPlace.description)
                    intent.putExtra("HappyPlaceDateKey", happyPlace.date)
                    intent.putExtra("HappyPlaceImageKey", happyPlace.imageUri)
                    intent.putExtra("HappyPlaceLocationKey", happyPlace.location)
                    intent.putExtra("HappyPlaceDetails", true)
                    startActivity(intent)
                    binding.recyclerView.adapter?.notifyItemChanged(viewHolder.adapterPosition)
                } else {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            happyPlaceDAO!!.deleteHappyPlace(happyPlace)
                        }

                        val snackbar =
                            Snackbar.make(binding.recyclerView, "Happy place is deleted!", 3000)
                                .setAction("UNDO") {
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) {
                                            happyPlaceDAO?.addHappyPlace(happyPlace)
                                        }
                                    }
                                    val snackbarOne =
                                        Snackbar.make(
                                            binding.recyclerView,
                                            "Happy Place is restored!",
                                            1500
                                        )
                                    snackbarOne.setBackgroundTint(Color.BLACK)
                                    val viewTwo = snackbarOne.view
                                    val tv =
                                        viewTwo.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                                    if (tv != null) {
                                        tv.textSize = 16f
                                        tv.setBackgroundColor(Color.BLACK)
                                        tv.setTextColor(
                                            ContextCompat.getColor(
                                                this@MainActivity,
                                                R.color.background
                                            )
                                        )
                                    }
                                    snackbarOne.show()
                                }

                        val view = snackbar.view
                        snackbar.view.setBackgroundColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                R.color.black
                            )
                        )
                        snackbar.setBackgroundTint(Color.BLACK)
                        val tv =
                            view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        val actionTextView =
                            view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
                        if (actionTextView != null) {
                            actionTextView.textSize = 18f
                            actionTextView.setBackgroundColor(Color.BLACK)
                            actionTextView.setTextColor(
                                ContextCompat.getColor(
                                    this@MainActivity,
                                    R.color.green
                                )
                            )
                        }
                        if (tv != null) {
                            tv.setBackgroundColor(Color.BLACK)
                            tv.textSize = 16f
                            tv.setTextColor(
                                ContextCompat.getColor(
                                    this@MainActivity,
                                    R.color.background
                                )
                            )
                        }
                        view.setBackgroundColor(Color.BLACK)
                        snackbar.show()
                    }
                    binding.recyclerView.adapter?.notifyItemChanged(viewHolder.adapterPosition)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipe)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

}