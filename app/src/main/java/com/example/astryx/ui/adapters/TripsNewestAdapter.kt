package com.example.astryx.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.data.entities.Trip
import com.example.astryx.data.utils.autoNotify

class TripsNewestAdapter(
    private var trips: List<Trip>,
    private val onCardClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripsNewestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.item_image)
        val title: TextView = view.findViewById(R.id.trip_title_item)
        val tripDates: TextView = view.findViewById(R.id.trip_date_item_newest_trip)
        val stops: TextView = view.findViewById(R.id.trip_stop_count)
        val tripCard: View = view.findViewById(R.id.newest_trip_card_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_newest, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = trips[position]
        holder.title.text = item.title
        holder.stops.text = "${item.destinationIds.size} stops"

        val start = item.getFormattedStartDate()
        val end = item.getFormattedEndDate()

        if (start != null && end != null) {
            holder.tripDates.text = "$start - $end"
            holder.tripDates.visibility = View.VISIBLE
        } else {
            holder.tripDates.visibility = View.GONE
        }

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.tripCard.setOnClickListener { onCardClick(item) }
    }

    override fun getItemCount() = trips.size

    fun updateData(newList: List<Trip>) {
        val oldList = this.trips
        this.trips = newList
        autoNotify(oldList, newList) { old, new -> old.id == new.id }
    }
}
