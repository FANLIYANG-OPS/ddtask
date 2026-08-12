package com.ddtask.scheduler.util

import android.content.Context

/** 屏幕常亮/最暗、亮度与熄屏超时备份值，以及权限向导完成标记。 */
class SettingsStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    var dimScreen: Boolean
        get() = prefs.getBoolean(KEY_DIM_SCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_DIM_SCREEN, value).apply()

    var savedBrightness: Int
        get() = prefs.getInt(KEY_SAVED_BRIGHTNESS, -1)
        set(value) = prefs.edit().putInt(KEY_SAVED_BRIGHTNESS, value).apply()

    var savedScreenTimeout: Int
        get() = prefs.getInt(KEY_SAVED_SCREEN_TIMEOUT, -1)
        set(value) = prefs.edit().putInt(KEY_SAVED_SCREEN_TIMEOUT, value).apply()

    var permissionSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSION_SETUP_COMPLETED, value).apply()

    companion object {
        private const val PREFS_NAME = "ddtask_settings"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_DIM_SCREEN = "dim_screen"
        private const val KEY_SAVED_BRIGHTNESS = "saved_brightness"
        private const val KEY_SAVED_SCREEN_TIMEOUT = "saved_screen_timeout"
        private const val KEY_PERMISSION_SETUP_COMPLETED = "permission_setup_completed"
    }
}
