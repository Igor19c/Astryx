package com.example.astryx.data.entities

import android.view.View
import com.example.astryx.R

data class UIConfig(
    val title: String = "",
    val isBottomNavVisible: Boolean = true,
    val isHeaderVisible: Boolean = true,

    // Left Button Control
    val isLeftBtnVisible: Boolean = true,
    val leftIconRes: Int = R.drawable.ic_settings,
    val onLeftClick: (() -> Unit)? = null,

    // Right Button Control
    val isRightBtnVisible: Boolean = true,
    val rightIconRes: Int = R.drawable.ic_search,
    val onRightClick: (() -> Unit)? = null,

    val selectedTabId: Int? = null,

    // Custom Header View
    val customHeaderView: View? = null,

    val customSearchView: View? = null
)
