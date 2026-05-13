package com.example.astryx.data.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.astryx.R

class SettingsManager(private val context: Context) {
    private var prefs: SharedPreferences =
        context.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE)
    private var currentUid: String? = null

    companion object {
        private const val GLOBAL_PREFS = "global_settings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ACCENT_THEME = "accent_theme"
        private const val KEY_NEEDS_RAUTH = "needs_rauth"
    }

    var needsReAuthAfterPasswordReset: Boolean
        get() = prefs.getBoolean(KEY_NEEDS_RAUTH, false)
        set(value) = prefs.edit().putBoolean(KEY_NEEDS_RAUTH, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            applyDarkMode(value)
        }

    var selectedThemeResId: Int
        get() = prefs.getInt(KEY_ACCENT_THEME, R.style.Theme_Astryx)
        set(value) {
            prefs.edit().putInt(KEY_ACCENT_THEME, value).apply()
        }

    fun switchUser(uid: String?) {
        currentUid = uid
        val prefsName = if (uid != null) "settings_$uid" else GLOBAL_PREFS
        prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        applyDarkMode(isDarkMode)
    }

    fun resetToDefaults() {
        switchUser(null)
        prefs.edit().clear().apply()
    }

    fun getThemeName(): String {
        return when (selectedThemeResId) {
            R.style.Theme_Astryx -> context.getString(R.string.ac_indigo)
            R.style.Theme_Astryx_Blue -> context.getString(R.string.ac_blue)
            R.style.Theme_Astryx_Red -> context.getString(R.string.ac_crimson)
            R.style.Theme_Astryx_Green -> context.getString(R.string.ac_green)
            else -> "Space Color"
        }
    }

    fun applyDarkMode(isDark: Boolean) {
        val mode =
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun applyTheme(activityContext: Context) {
        activityContext.setTheme(selectedThemeResId)
    }
}