package com.example.astryx.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.utils.IconHelper.setupDifficultyIcons
import com.example.astryx.data.utils.autoNotify

class TripDestinationsAdapter(
    private var destinations: MutableList<Destination>,
    private val onImageClick: (ImageView, String) -> Unit
) : RecyclerView.Adapter<TripDestinationsAdapter.ViewHolder>() {

    private var isEditMode: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: View = view.findViewById(R.id.card_container)
        val image: ImageView = view.findViewById(R.id.trip_destination_image)
        val title: TextView = view.findViewById(R.id.trip_destination_title)
        val subtitle: TextView = view.findViewById(R.id.trip_destination_subtitle)
        val ratings: TextView = view.findViewById(R.id.trip_destination_rating)
        val difficultyIconContainer: LinearLayout =
            view.findViewById(R.id.trip_dest_difficulty_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_destination, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = destinations[position]

        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        setupDifficultyIcons(holder.difficultyIconContainer, item.difficulty)

        holder.ratings.text = String.format("%.1f", item.ratingAvg)

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_home_earth)
            .into(holder.image)

        holder.image.setOnClickListener {
            if (item.imageUrl.isNotEmpty()) {
                onImageClick(holder.image, item.imageUrl)
            }
        }

        resetSwipeState(holder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("RESET_SWIPE")) {
            resetSwipeState(holder)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun resetSwipeState(holder: ViewHolder) {
        holder.cardContainer.translationX = 0f
        holder.itemView.alpha = 1f
    }

    override fun getItemCount() = destinations.size

    fun updateData(newList: List<Destination>) {
        val oldList = this.destinations
        this.destinations = newList.toMutableList()
        autoNotify(oldList, newList) { old, new -> old.id == new.id }
    }

    fun setEditMode(enabled: Boolean) {
        this.isEditMode = enabled
        notifyDataSetChanged()
    }

    fun getDestinationAt(position: Int): Destination {
        return destinations[position]
    }
}
