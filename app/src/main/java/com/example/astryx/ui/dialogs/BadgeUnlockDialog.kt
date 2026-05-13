package com.example.astryx.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.example.astryx.data.entities.Badge
import com.example.astryx.databinding.DialogBadgeUnlockedBinding
import java.util.LinkedList
import java.util.Queue

class BadgeUnlockDialog(private val context: Context) {

    private val badgeQueue: Queue<Badge> = LinkedList()
    private var isShowing = false

    fun showBadges(badges: List<Badge>) {
        badgeQueue.addAll(badges)
        showNext()
    }

    private fun showNext() {
        if (isShowing || badgeQueue.isEmpty()) return
        val badge = badgeQueue.poll() ?: return
        displayDialog(badge)
    }

    private fun displayDialog(badge: Badge) {
        isShowing = true
        val dialog = Dialog(context)
        val binding = DialogBadgeUnlockedBinding.inflate(LayoutInflater.from(context))

        dialog.setContentView(binding.root)
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        binding.dialogBadgeName.text = badge.name
        binding.dialogBadgeDescription.text = badge.description

        val assetPath = "file:///android_asset/${badge.assetPath}"
        Glide.with(context).load(assetPath).into(binding.dialogBadgeImage)

        binding.dialogCloseBtn.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener {
            isShowing = false
            showNext()
        }

        dialog.show()
    }
}