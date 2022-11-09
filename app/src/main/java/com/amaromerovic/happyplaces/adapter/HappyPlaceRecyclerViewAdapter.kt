package com.amaromerovic.happyplaces.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amaromerovic.happyplaces.databinding.RecyclerViewItemBinding
import com.amaromerovic.happyplaces.model.HappyPlaceModel

class HappyPlaceRecyclerViewAdapter(
    val happyPlaces: List<HappyPlaceModel>,
    val onHappyPlaceClickListener: OnHappyPlaceClickListener
) :
    RecyclerView.Adapter<HappyPlaceRecyclerViewAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RecyclerViewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), onHappyPlaceClickListener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val happyPlace = happyPlaces[position]
        holder.binding.titleTextView.text = happyPlace.title
        holder.binding.descriptionTextView.text = happyPlace.description
        holder.binding.imageView.setImageURI(Uri.parse(happyPlace.imageUri))
    }


    override fun getItemCount(): Int {
        return happyPlaces.size
    }

    class ViewHolder(
        val binding: RecyclerViewItemBinding,
        private val onHappyPlaceClickListener: OnHappyPlaceClickListener
    ) : RecyclerView.ViewHolder(binding.root), OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onHappyPlaceClickListener.onHappyPlaceClicked(adapterPosition)
        }
    }


    interface OnHappyPlaceClickListener {
        fun onHappyPlaceClicked(position: Int)
    }
}