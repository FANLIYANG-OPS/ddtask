package com.ddtask.scheduler.util

import android.content.Context
import android.provider.Settings

/** 读写系统屏幕亮度；关闭「屏幕最暗」时恢复用户原亮度。 */
object BrightnessController {

    private const val MIN_BRIGHTNESS = 1

    fun canWriteSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    fun saveCurrentBrightness(context: Context, settingsStorage: SettingsStorage) {
        if (settingsStorage.savedBrightness >= 0) return
        val current = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            128
        )
        settingsStorage.savedBrightness = current
    }

    fun setMinimumBrightness(context: Context) {
        if (!canWriteSettings(context)) return
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            MIN_BRIGHTNESS
        )
    }

    fun restoreBrightness(context: Context, settingsStorage: SettingsStorage) {
        if (!canWriteSettings(context)) return
        val saved = settingsStorage.savedBrightness
        if (saved >= 0) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                saved
            )
            settingsStorage.savedBrightness = -1
        }
    }
}
