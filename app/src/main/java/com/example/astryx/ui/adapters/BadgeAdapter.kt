package com.example.astryx.ui.adapters

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.databinding.ItemBadgeBinding
import com.example.astryx.data.entities.Badge
import com.example.astryx.data.utils.autoNotify
import androidx.core.view.isVisible

class BadgeAdapter(
    private var allBadges: List<Badge> = listOf(),
    private var earnedBadgeIds: Set<String> = emptySet()
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = allBadges[position]
        val isEarned = earnedBadgeIds.contains(badge.id)

        holder.binding.badgeName.text = if (isEarned) badge.name else "Locked"

        holder.binding.badgeCard.setOnClickListener {
            holder.binding.badgeDescriptionText.text = badge.description
            holder.binding.badgeDescriptionLayout.visibility =
                if (holder.binding.badgeDescriptionLayout.isVisible) View.GONE else View.VISIBLE
        }

        val assetPath = "file:///android_asset/${badge.assetPath}"

        if (isEarned) {
            holder.binding.badgeImage.clearColorFilter()
            holder.binding.badgeImage.alpha = 1.0f
        } else {
            holder.binding.badgeImage.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            holder.binding.badgeImage.alpha = 0.6f
        }

        Glide.with(holder.itemView.context)
            .load(assetPath)
            .centerCrop()
            .into(holder.binding.badgeImage)
    }

    override fun getItemCount(): Int = allBadges.size

    fun updateData(newAllBadges: List<Badge>, newEarnedBadgeIds: List<String>) {
        val oldBadges = this.allBadges
        val oldEarnedIds = this.earnedBadgeIds

        this.allBadges = newAllBadges
        this.earnedBadgeIds = newEarnedBadgeIds.toSet()

        autoNotify(
            oldList = oldBadges,
            newList = newAllBadges,
            compareItems = { old, new -> old.id == new.id },
            compareContents = { old, new ->
                val wasEarned = oldEarnedIds.contains(old.id)
                val isNowEarned = newEarnedBadgeIds.contains(new.id)
                old == new && wasEarned == isNowEarned
            }
        )
    }

    class BadgeViewHolder(val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root)
}
