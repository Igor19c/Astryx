package com.example.astryx.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.databinding.ItemReviewBinding
import com.example.astryx.data.entities.Review
import com.example.astryx.data.utils.IconHelper
import com.example.astryx.data.utils.autoNotify
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(
    private var reviews: List<Review> = listOf()
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    fun updateData(newReviews: List<Review>) {
        val oldReviews = this.reviews
        this.reviews = newReviews
        autoNotify(oldReviews, newReviews) { old, new -> old.id == new.id }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {

            binding.reviewerNameItemReview.text = review.username
            binding.destinationTitleItemReview.text = review.destinationTitle
            binding.reviewContentItemReview.text =
                if (review.comment.isNotEmpty()) "\"${review.comment}\"" else ""

            Glide.with(binding.userPicItemReview.context)
                .load(review.userProfileImage)
                .placeholder(R.drawable.pic_profile_placeholder)
                .circleCrop()
                .into(binding.userPicItemReview)

            binding.reviewDateItemReview.text = review.createdAt?.let {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
            } ?: ""
            
            val stars = listOf(
                binding.star1,
                binding.star2,
                binding.star3,
                binding.star4,
                binding.star5
            )

            IconHelper.setupRatingStars(stars, review.rating.toDouble())
        }
    }

}