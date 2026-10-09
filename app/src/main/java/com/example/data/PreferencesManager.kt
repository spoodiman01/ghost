package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ghost_touch_shield_prefs", Context.MODE_PRIVATE)

    var isShieldEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHIELD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SHIELD_ENABLED, value).apply()

    var isFloatingToolEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_TOOL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_TOOL_ENABLED, value).apply()

    var isPreviewOverlayVisible: Boolean
        get() = prefs.getBoolean(KEY_PREVIEW_OVERLAY_VISIBLE, false)
        set(value) = prefs.edit().putBoolean(KEY_PREVIEW_OVERLAY_VISIBLE, value).apply()

    var autoStartOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_BOOT, value).apply()

    var calibrationDurationSeconds: Int
        get() = prefs.getInt(KEY_CALIBRATION_DURATION, 15)
        set(value) = prefs.edit().putInt(KEY_CALIBRATION_DURATION, value).apply()

    companion object {
        private const val KEY_SHIELD_ENABLED = "shield_enabled"
        private const val KEY_FLOATING_TOOL_ENABLED = "floating_tool_enabled"
        private const val KEY_PREVIEW_OVERLAY_VISIBLE = "preview_overlay_visible"
        private const val KEY_AUTO_START_BOOT = "auto_start_boot"
        private const val KEY_CALIBRATION_DURATION = "calibration_duration"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
