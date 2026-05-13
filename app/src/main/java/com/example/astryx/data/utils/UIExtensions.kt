package com.example.astryx.data.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.InputStream

enum class LoadingType { PROGRESS_BAR, LOTTIE }
enum class GradientDirection { VERTICAL, HORIZONTAL, DIAGONAL }

private const val LOADING_OVERLAY_TAG = "loading_overlay"

fun String.toCapitalized(): String {
    if (this.isBlank()) return ""
    val cleanString = this.replace("_", " ")
    val words = cleanString.lowercase().split(" ")
    return words.joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
}

/**
 *  Activity & Window Extensions
 */
fun Activity.hideSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}


/**
 *  Chip Extensions
 */
fun ChipGroup.addTags(tags: List<String>) {
    this.removeAllViews()
    tags.forEach { tag ->
        val chip = LayoutInflater.from(context)
            .inflate(R.layout.layout_single_chip, this, false) as Chip
        chip.text = tag.toCapitalized()
        chip.isClickable = false
        this.addView(chip)
    }
}

fun setupDifficultyChip(chip: TextView, difficulty: Int) {
    val context = chip.context
    val (diffText, colorRes, bgRes) = when (difficulty) {
        1 -> Triple("Easy", R.color.success, R.drawable.bg_success_stroke)
        2 -> Triple("Medium", R.color.achievement, R.drawable.bg_achievement_stroke)
        3 -> Triple("Hard", R.color.alert, R.drawable.bg_alert_stroke)
        else -> Triple("", R.color.gray_dark, R.drawable.bg_gray_stroke)
    }
    chip.text = diffText
    chip.setTextColor(context.getColorStateList(colorRes))
    chip.background = ContextCompat.getDrawable(context, bgRes)
}

/**
 *  Scroll Effects
 */
fun NestedScrollView.setupFadeOnScroll(views: List<View>) {
    this.setOnScrollChangeListener { _, _, scrollY, _, _ ->
        views.forEach { view ->
            val relativeScroll = scrollY - view.top

            if (relativeScroll > 0) {
                val alpha = 1f - (relativeScroll.toFloat() / 500f)
                view.alpha = alpha.coerceIn(0f, 1f)

                view.translationY = relativeScroll * 0.3f
            } else {
                view.alpha = 1f
                view.translationY = 0f
            }
        }
    }
}

/**
 *  Gradient Extensions
 */
fun ImageView.applyGradientTint(
    startColor: Int = context.getColorFromAttr(R.attr.customColorPrimary),
    endColor: Int = context.getColorFromAttr(R.attr.customColorPrimaryVariant),
    direction: GradientDirection = GradientDirection.VERTICAL
) {
    val original = drawable ?: return

    val gradientWrapper = object : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun draw(canvas: Canvas) {
            val b = bounds
            if (b.isEmpty) return

            val (x1, y1) = when (direction) {
                GradientDirection.VERTICAL -> 0f to b.height().toFloat()
                GradientDirection.HORIZONTAL -> b.width().toFloat() to 0f
                GradientDirection.DIAGONAL -> b.width().toFloat() to b.height()
                    .toFloat()
            }

            paint.shader = LinearGradient(
                b.left.toFloat(), b.top.toFloat(),
                b.left + x1, b.top + y1,
                startColor, endColor, Shader.TileMode.CLAMP
            )

            val count = canvas.saveLayer(
                b.left.toFloat(), b.top.toFloat(),
                b.right.toFloat(), b.bottom.toFloat(), null
            )

            original.bounds = b
            original.draw(canvas)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawRect(b, paint)
            paint.xfermode = null

            canvas.restoreToCount(count)
        }

        override fun setAlpha(alpha: Int) {
            original.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            original.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        override fun getIntrinsicWidth(): Int = original.intrinsicWidth
        override fun getIntrinsicHeight(): Int = original.intrinsicHeight
    }

    setImageDrawable(gradientWrapper)
}

fun TextView.applyGradientText(
    startColor: Int = context.getColorFromAttr(R.attr.customColorPrimary),
    endColor: Int = context.getColorFromAttr(R.attr.customColorPrimaryVariant),
    direction: GradientDirection = GradientDirection.HORIZONTAL
) {
    post {
        val width = paint.measureText(text.toString())
        val height = textSize * lineCount

        val (x1, y1) = when (direction) {
            GradientDirection.VERTICAL -> 0f to height
            GradientDirection.HORIZONTAL -> width to 0f
            GradientDirection.DIAGONAL -> width to height
        }

        val shader = LinearGradient(
            0f, 0f, x1, y1,
            intArrayOf(startColor, endColor),
            null,
            Shader.TileMode.CLAMP
        )
        this.paint.shader = shader
        invalidate()
    }
}


/**
 *  Loading & Message Extensions
 */
fun Activity.showLoading(
    message: String? = null,
    type: LoadingType = LoadingType.LOTTIE
) {
    val root = findViewById<ViewGroup>(android.R.id.content) ?: return
    if (root.findViewWithTag<View>(LOADING_OVERLAY_TAG) != null) return

    val layoutRes = when (type) {
        LoadingType.PROGRESS_BAR -> R.layout.layout_loading_progress_bar_overlay
        LoadingType.LOTTIE -> R.layout.layout_loading_lottie_overlay
    }

    val loadingOverlay = LayoutInflater.from(this).inflate(layoutRes, root, false)
    loadingOverlay.tag = LOADING_OVERLAY_TAG

    message?.let {
        loadingOverlay.findViewById<TextView>(
            if (type == LoadingType.PROGRESS_BAR) R.id.loading_message_progress_bar
            else R.id.loading_message_lottie
        )?.text = it
    }

    root.addView(loadingOverlay)
}

fun Activity.hideLoading() {
    val root = findViewById<ViewGroup>(android.R.id.content) ?: return
    root.findViewWithTag<View>(LOADING_OVERLAY_TAG)?.let { root.removeView(it) }
}

private fun showCustomMessageInternal(
    root: ViewGroup,
    title: String,
    body: String,
    duration: Long
) {
    val context = root.context
    val messageView =
        LayoutInflater.from(context)
            .inflate(R.layout.layout_custom_message, root, false)

    messageView.findViewById<TextView>(R.id.message_title).text = title
    messageView.findViewById<TextView>(R.id.message_body).text = body

    root.addView(messageView)

    messageView.alpha = 0f
    messageView.translationY = -100f
    messageView.animate().alpha(1f).translationY(0f).setDuration(400).start()

    Handler(Looper.getMainLooper()).postDelayed({
        if (messageView.parent != null) {
            messageView.animate()
                .alpha(0f)
                .translationY(-100f)
                .setDuration(400)
                .withEndAction { root.removeView(messageView) }
                .start()
        }
    }, duration)
}

fun Activity.showCustomMessage(title: String, body: String, duration: Long = 3000) {
    val root = findViewById<ViewGroup>(android.R.id.content) ?: return
    showCustomMessageInternal(root, title, body, duration)
}

fun Fragment.showCustomMessage(title: String, body: String, duration: Long = 3000) {
    activity?.showCustomMessage(title, body, duration)
}

fun Context.showCustomMessage(title: String, body: String, duration: Long = 3000) {
    (this as? Activity)?.showCustomMessage(title, body, duration)
}

/**
 *  Context & Resource Extensions
 */
@ColorInt
fun Context.getColorFromAttr(@AttrRes attrColor: Int): Int {
    val typedValue = TypedValue()
    if (theme.resolveAttribute(attrColor, typedValue, true)) {
        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(this, typedValue.resourceId)
        } else {
            typedValue.data
        }
    }
    theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
    return typedValue.data
}

fun Context.openUriInputStream(uri: Uri): InputStream? {
    return try {
        val uriString = uri.toString()
        if (uriString.contains("android_asset/")) {
            val path = uriString.substringAfter("android_asset/").trimStart('/')
            assets.open(path)
        } else {
            contentResolver.openInputStream(uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


/**
 *  Activity & Window Extensions
 */
fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showAsFullScreen(imageUrl: String) {
    val context = this.context
    val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    dialog.setContentView(R.layout.layout_full_image)

    val container = dialog.findViewById<View>(R.id.dialog_full_image)
    val fullImageView = dialog.findViewById<ImageView>(R.id.full_image_view)

    if (fullImageView == null || container == null) return

    Glide.with(context).load(imageUrl).into(fullImageView)

    val screenLocation = IntArray(2)
    this.getLocationOnScreen(screenLocation)

    val displayMetrics = context.resources.displayMetrics

    fullImageView.apply {
        pivotX = 0f
        pivotY = 0f
        scaleX = this@showAsFullScreen.width.toFloat() / displayMetrics.widthPixels
        scaleY =
            this@showAsFullScreen.height.toFloat() / displayMetrics.heightPixels
        translationX = screenLocation[0].toFloat()
        translationY = screenLocation[1].toFloat()
    }
    container.alpha = 0f

    dialog.show()

    fullImageView.animate()
        .scaleX(1f)
        .scaleY(1f)
        .translationX(0f)
        .translationY(0f)
        .setDuration(300)
        .start()

    container.animate().alpha(1f).setDuration(300).start()

    fullImageView.setOnClickListener {
        fullImageView.animate()
            .scaleX(this@showAsFullScreen.width.toFloat() / displayMetrics.widthPixels)
            .scaleY(this@showAsFullScreen.height.toFloat() / displayMetrics.heightPixels)
            .translationX(screenLocation[0].toFloat())
            .translationY(screenLocation[1].toFloat())
            .setDuration(300)
            .withEndAction { dialog.dismiss() }
            .start()
        container.animate().alpha(0f).setDuration(300).start()
    }
}


/**
 *  RecyclerView & Adapter Extensions
 */
fun <T> RecyclerView.Adapter<*>.autoNotify(
    oldList: List<T>,
    newList: List<T>,
    compareContents: (T, T) -> Boolean = { a, b -> a == b },
    compareItems: (T, T) -> Boolean
) {
    val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            return compareItems(oldList[oldPos], newList[newPos])
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return compareContents(oldList[oldPos], newList[newPos])
        }
    })
    diff.dispatchUpdatesTo(this)
}

fun <T : RecyclerView.ViewHolder> RecyclerView.setupSwipeAction(
    getCardContainer: (T) -> View,
    canSwipe: (() -> Boolean)? = null,
    onSwiped: (Int) -> Unit
) {
    val callback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                t: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val enabled = canSwipe?.invoke() ?: true
                return if (enabled) super.getSwipeDirs(
                    recyclerView,
                    viewHolder
                ) else 0
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                onSwiped(viewHolder.adapterPosition)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val card = getCardContainer(vh as T)
                    card.translationX = dX
                } else {
                    super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
                }
            }
        }
    ItemTouchHelper(callback).attachToRecyclerView(this)
}

/**
 *  Icon Extensions
 */

object IconHelper {
    fun setupDifficultyIcons(container: LinearLayout, level: Int) {
        container.removeAllViews()
        val context = container.context
        val icons = listOf(
            R.drawable.ic_easy_emoji,
            R.drawable.ic_medium_emoji,
            R.drawable.ic_hard_emoji
        )
        for (i in 1..3) {
            val iconView = ImageView(context)
            val size = (16 * context.resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(0, 0, 10, 0)
            iconView.layoutParams = params
            iconView.setImageResource(icons[i - 1])
            val tintColor = if (i == level) {
                when (level) {
                    1 -> ContextCompat.getColor(context, R.color.success)
                    2 -> ContextCompat.getColor(context, R.color.achievement)
                    3 -> ContextCompat.getColor(context, R.color.alert)
                    else -> ContextCompat.getColor(context, R.color.body)
                }
            } else {
                ContextCompat.getColor(context, R.color.body)
            }
            iconView.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            container.addView(iconView)
        }
    }

    fun setupPriceTierIcons(container: LinearLayout, level: Int) {
        container.removeAllViews()
        val context = container.context
        for (i in 1..4) {
            val icon = ImageView(context)
            val size = (18 * context.resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            if (i < 4) params.setMargins(0, 0, -6, 0)
            icon.layoutParams = params
            icon.setImageResource(R.drawable.ic_dollar_banknote)
            val tintColor = if (i <= level) {
                ContextCompat.getColor(context, R.color.space_green_light)
            } else {
                ContextCompat.getColor(context, R.color.space_green_light_faded)
            }
            icon.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            container.addView(icon)
        }
    }

    fun setupSafetyIcons(container: LinearLayout, level: Int) {
        container.removeAllViews()
        val context = container.context
        for (i in 1..5) {
            val iconView = ImageView(context)
            val size = (17 * context.resources.displayMetrics.density).toInt()
            iconView.layoutParams = LinearLayout.LayoutParams(size, size)
            iconView.setImageResource(R.drawable.ic_hazard)
            val tintColor = if (i <= level) {
                ContextCompat.getColor(context, R.color.space_red)
            } else {
                ContextCompat.getColor(context, R.color.space_red_faded)
            }
            iconView.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            container.addView(iconView)
        }
    }

    fun setupRatingStars(stars: List<ImageView>, rating: Double) {
        stars.forEachIndexed { index, imageView ->
            val starLevel = index + 1
            val iconRes = when {
                rating >= starLevel -> R.drawable.ic_rating_star_filled
                rating >= starLevel - 0.5 -> R.drawable.ic_rating_star_half
                else -> R.drawable.ic_rating_star_outlined
            }
            imageView.setImageResource(iconRes)
            val tintColor =
                imageView.context.getColorFromAttr(R.attr.customColorPrimary)
            imageView.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        }
    }

    fun updateStarsInContainer(container: LinearLayout, rating: Double) {
        val stars = mutableListOf<ImageView>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is ImageView) {
                stars.add(child)
            }
        }
        setupRatingStars(stars, rating)
    }
}
