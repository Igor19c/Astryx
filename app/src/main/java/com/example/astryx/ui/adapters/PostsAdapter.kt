package com.example.astryx.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.data.entities.Post
import com.example.astryx.data.utils.IconHelper
import com.example.astryx.data.utils.autoNotify
import com.google.android.material.button.MaterialButton

class PostsAdapter(
    private var posts: MutableList<Post>,
    private val currentUserId: String?,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userProfileImage: ImageView = view.findViewById(R.id.user_profile_image_post_item)
        val image: ImageView = view.findViewById(R.id.image_post_item)
        val username: TextView = view.findViewById(R.id.user_name_post_item)
        val title: TextView = view.findViewById(R.id.trip_title_post_item)
        val likesCounter: TextView = view.findViewById(R.id.like_counter_post_item)
        val description: TextView = view.findViewById(R.id.description_post_item)
        val settingsBtn: MaterialButton = view.findViewById(R.id.settings_btn_post_item)
        val likeBtn: MaterialButton = view.findViewById(R.id.like_btn_post_item)

        val stars = listOf<ImageView>(
            view.findViewById(R.id.rating_star_01_post_item),
            view.findViewById(R.id.rating_star_02_post_item),
            view.findViewById(R.id.rating_star_03_post_item),
            view.findViewById(R.id.rating_star_04_post_item),
            view.findViewById(R.id.rating_star_05_post_item)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = posts[position]
        holder.title.text = item.title
        holder.likesCounter.text = item.likes.toString()
        holder.description.text = item.description
        holder.username.text = item.username

        IconHelper.setupRatingStars(holder.stars, item.rating.toDouble())

        val isLiked = currentUserId != null && item.likedBy.contains(currentUserId)
        val likeIcon = if (isLiked) R.drawable.ic_star_filled else R.drawable.ic_star_outlined
        holder.likeBtn.setIconResource(likeIcon)

        Glide.with(holder.itemView.context)
            .load(item.userProfileImage)
            .placeholder(R.drawable.pic_profile_placeholder)
            .circleCrop()
            .into(holder.userProfileImage)

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(holder.image)
        }

        holder.likeBtn.setOnClickListener { onLikeClick(item) }

        val isMyPost = currentUserId != null && item.uid == currentUserId
        holder.settingsBtn.visibility = if (isMyPost) View.VISIBLE else View.GONE
        holder.settingsBtn.setOnClickListener { view -> showSettingsPopup(view, item) }
    }

    private fun showSettingsPopup(anchorView: View, post: Post) {
        val context = anchorView.context
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.container_post_setings_btn, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.findViewById<MaterialButton>(R.id.delete_btn_post_item).setOnClickListener {
            onDeleteClick(post)
            popupWindow.dismiss()
        }

        popupView.findViewById<MaterialButton>(R.id.edit_btn_post_item).setOnClickListener {
            onEditClick(post)
            popupWindow.dismiss()
        }

        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(anchorView, -300, 0)
    }

    override fun getItemCount() = posts.size

    fun updateData(newList: List<Post>) {
        val oldList = this.posts
        this.posts = newList.toMutableList()
        autoNotify(oldList, newList) { old, new -> old.id == new.id }
    }
}