package com.example.astryx.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.data.entities.Destination
import com.example.astryx.R
import com.example.astryx.data.utils.autoNotify

class CarouselAdapter(private var items: List<Destination>) :
    RecyclerView.Adapter<CarouselAdapter.ViewHolder>() {

    private var onItemClickListener: ((Destination) -> Unit)? = null

    fun updateData(newItems: List<Destination>) {
        val oldList = this.items
        this.items = newItems
        autoNotify(oldList, newItems) { old, new -> old.id == new.id }
    }


    fun getItem(position: Int): Destination = items[position]

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.dest_image_carousel_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(item)
        }
    }

    override fun getItemCount() = items.size
}