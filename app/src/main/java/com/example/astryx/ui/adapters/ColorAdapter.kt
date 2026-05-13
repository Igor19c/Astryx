package com.example.astryx.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.astryx.R

class ColorAdapter(
    private val colors: List<Pair<String, Int>>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val circle: View = view.findViewById(R.id.color_circle)
        val name: TextView = view.findViewById(R.id.color_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (colorName, colorRes) = colors[position]
        holder.name.text = colorName

        val context = holder.itemView.context
        holder.circle.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

        holder.itemView.setOnClickListener { onColorSelected(position) }
    }

    override fun getItemCount() = colors.size
}
