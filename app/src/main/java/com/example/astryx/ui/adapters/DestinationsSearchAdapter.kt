package com.example.astryx.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.databinding.ItemExploreSearchBinding
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.utils.addTags
import com.example.astryx.data.utils.autoNotify
import com.example.astryx.data.utils.setupDifficultyChip

class DestinationsSearchAdapter(
    private var destinations: List<Destination>,
    private val onDetailsClick: (Destination) -> Unit
) : RecyclerView.Adapter<DestinationsSearchAdapter.SearchDestinationsViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchDestinationsViewHolder {
        val binding = ItemExploreSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SearchDestinationsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SearchDestinationsViewHolder,
        position: Int
    ) {
        val item = destinations[position]
        holder.bind(item)
    }

    inner class SearchDestinationsViewHolder(private val binding: ItemExploreSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(destination: Destination) {
            binding.titleItemExploreSearch.text = destination.title
            binding.subtitleItemExploreSearch.text = destination.subtitle
            binding.descriptionItemExploreSearch.text = destination.shortDescription
            binding.typeChipItemExploreSearch.text = destination.type
            binding.travelTimeItemExploreSearch.text =
                "Travel time ${destination.getTravelTimeInt()} days"
            binding.distanceItemExploreSearch.text = destination.distanceFromEarth

            if (destination.imageUrl.isNotEmpty()) {
                binding.imageItemExploreSearch.apply {
                    scaleType = if (destination.type == "PLANET")
                        ImageView.ScaleType.CENTER_INSIDE else ImageView.ScaleType.CENTER_CROP

                    Glide.with(context)
                        .load(destination.imageUrl)
                        .placeholder(R.drawable.ic_home_earth)
                        .into(this)
                }
            }

            setupDifficultyChip(
                binding.difficultyChipItemExploreSearch,
                destination.difficulty
            )

            binding.destTagsChipGroupItemExploreSearch.addTags(destination.tags)

            binding.btnViewDetailItemExploreSearch.setOnClickListener {
                onDetailsClick(destination)
            }
        }
    }

    override fun getItemCount() = destinations.size

    fun updateData(newList: List<Destination>) {
        val oldList = this.destinations
        this.destinations = newList.toMutableList()
        autoNotify(oldList, newList) { old, new -> old.id == new.id }
    }

}
