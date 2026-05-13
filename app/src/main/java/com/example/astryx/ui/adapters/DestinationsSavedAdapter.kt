package com.example.astryx.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.utils.autoNotify

class DestinationsSavedAdapter(
    private var destinations: MutableList<Destination>
) : RecyclerView.Adapter<DestinationsSavedAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: View = view.findViewById(R.id.saved_dest_card_container)
        val image: ImageView = view.findViewById(R.id.saved_destination_image)
        val title: TextView = view.findViewById(R.id.saved_destination_title)
        val subtitle: TextView = view.findViewById(R.id.saved_destination_subtitle)
        val ratings: TextView = view.findViewById(R.id.saved_destination_rating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_destination, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = destinations[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.ratings.text = String.format("%.1f", item.ratingAvg)

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.cardContainer.translationX = 0f

    }

    override fun getItemCount() = destinations.size

    fun updateData(newList: List<Destination>) {
        val oldList = this.destinations
        this.destinations = newList.toMutableList()
        autoNotify(oldList, newList) { old, new -> old.id == new.id }
    }

    fun getDestinationAt(position: Int): Destination {
        return destinations[position]
    }


}
