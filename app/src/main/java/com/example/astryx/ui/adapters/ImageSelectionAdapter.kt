package com.example.astryx.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.data.utils.autoNotify
import com.example.astryx.databinding.ItemAvatarBinding

class ImageSelectionAdapter(
    private var images: List<String>,
    private val onPlusClick: () -> Unit,
    private val onImageSelected: (String) -> Unit
) : RecyclerView.Adapter<ImageSelectionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAvatarBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            holder.binding.avatarImage.setImageDrawable(null)
            holder.binding.plusIcon.visibility = View.VISIBLE
            holder.itemView.setOnClickListener { onPlusClick() }
        } else {
            holder.binding.plusIcon.visibility = View.GONE
            val imageUrl = images[position - 1]

            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.binding.avatarImage)

            holder.itemView.setOnClickListener { onImageSelected(imageUrl) }
        }
    }

    override fun getItemCount(): Int = images.size + 1

    fun updateData(newImages: List<String>) {
        val oldImages = this.images
        this.images = newImages
        autoNotify(oldImages, newImages) { old, new -> old == new }
    }
}
