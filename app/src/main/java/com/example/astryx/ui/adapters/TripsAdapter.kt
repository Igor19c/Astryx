package com.example.astryx.ui.adapters

import android.text.format.DateUtils
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
import com.google.android.material.button.MaterialButton

class TripsAdapter(
    private var trips: MutableList<Trip>,
    private val onOpenClick: (Trip) -> Unit,
    private val onConfirmClick: (Trip) -> Unit,
    private val onShareClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripsAdapter.ViewHolder>() {


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: View = view.findViewById(R.id.trip_card_item)
        val mainImage: ImageView = view.findViewById(R.id.trip_main_image_item_trip)
        val image1: ImageView = view.findViewById(R.id.trip_dest_image_1)
        val image2: ImageView = view.findViewById(R.id.trip_dest_image_2)
        val image3: ImageView = view.findViewById(R.id.trip_dest_image_3)
        val additionalCounter: TextView = view.findViewById(R.id.additional_trip_dest_counter)

        val title: TextView = view.findViewById(R.id.title_trip_item)
        val tripDates: TextView = view.findViewById(R.id.trip_dates_trip_item)
        val createdAt: TextView = view.findViewById(R.id.created_at_trip_item)
        val stopCount: TextView = view.findViewById(R.id.stop_count_trip_item)

        val draftChip: TextView = view.findViewById(R.id.draft_chip_trip_item)
        val plannedChip: TextView = view.findViewById(R.id.planned_chip_trip_item)
        val completedChip: TextView = view.findViewById(R.id.completed_chip_trip_item)

        val openBtn: MaterialButton = view.findViewById(R.id.open_btn_trip_item)
        val finalizeBtn: MaterialButton = view.findViewById(R.id.finalize_btn_trip_item)
        val shareBtn: MaterialButton = view.findViewById(R.id.share_btn_trip_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        val context = holder.itemView.context

        holder.title.text = trip.title.ifEmpty { "Unnamed Journey" }
        holder.stopCount.text = "${trip.destinationIds.size} stops"

        val start = trip.getFormattedStartDate()
        val end = trip.getFormattedEndDate()

        if (start != null && end != null) {
            holder.tripDates.text = "$start - $end"
            holder.tripDates.visibility = View.VISIBLE
        } else {
            holder.tripDates.visibility = View.GONE
        }

        trip.createdAt?.let {
            holder.createdAt.text = DateUtils.getRelativeTimeSpanString(it)
        }

        holder.draftChip.visibility = if (trip.isDraft) View.VISIBLE else View.GONE
        holder.plannedChip.visibility = if (trip.isPlanned && !trip.isCompleted) View.VISIBLE else View.GONE
        holder.completedChip.visibility = if (trip.isCompleted) View.VISIBLE else View.GONE

        if (trip.isDraft) {
            holder.openBtn.visibility = View.VISIBLE
            holder.finalizeBtn.visibility = View.VISIBLE
            holder.shareBtn.visibility = View.GONE
        } else {
            holder.openBtn.visibility = View.VISIBLE
            holder.finalizeBtn.visibility = View.GONE
            holder.shareBtn.visibility = View.VISIBLE
        }


        Glide.with(holder.mainImage.context)
            .load(trip.imageUrl)
            .placeholder(R.drawable.ic_home_earth)
            .into(holder.mainImage)

        val images = listOf(holder.image1, holder.image2, holder.image3)
        images.forEach { it.visibility = View.GONE }
        holder.additionalCounter.visibility = View.GONE

        trip.destinationImages.take(3).forEachIndexed { index, url ->
            images[index].visibility = View.VISIBLE
            Glide.with(context).load(url).placeholder(R.drawable.pic_profile_placeholder).circleCrop()
                .into(images[index])
        }

        if (trip.destinationIds.size > 3) {
            holder.additionalCounter.visibility = View.VISIBLE
            holder.additionalCounter.text = "+${trip.destinationIds.size - 3}"
        }

        holder.openBtn.setOnClickListener { onOpenClick(trip) }
        holder.finalizeBtn.setOnClickListener { onConfirmClick(trip) }
        holder.shareBtn.setOnClickListener { onShareClick(trip) }

        holder.cardContainer.translationX = 0f
        holder.itemView.alpha = 1f
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("RESET_SWIPE")) {
            holder.cardContainer.translationX = 0f
            holder.itemView.alpha = 1f
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount() = trips.size

    fun updateData(newList: List<Trip>) {
        val oldList = this.trips
        this.trips = newList.toMutableList()
        autoNotify(oldList, newList) { old, new -> old.id == new.id }
    }

    fun getTripAt(position: Int): Trip {
        return trips[position]
    }
}
