package com.ddtask.scheduler.util

import android.content.Context
import android.provider.Settings

object ScreenTimeoutController {

    /** 常亮期间使用的熄屏超时：30 分钟 */
    private const val KEEP_ON_TIMEOUT_MS = 30 * 60 * 1000

    fun saveAndExtend(context: Context, settingsStorage: SettingsStorage) {
        if (!BrightnessController.canWriteSettings(context)) return
        if (settingsStorage.savedScreenTimeout < 0) {
            val current = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                30_000
            )
            settingsStorage.savedScreenTimeout = current
        }
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            KEEP_ON_TIMEOUT_MS
        )
    }

    fun restore(context: Context, settingsStorage: SettingsStorage) {
        if (!BrightnessController.canWriteSettings(context)) return
        val saved = settingsStorage.savedScreenTimeout
        if (saved >= 0) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                saved
            )
            settingsStorage.savedScreenTimeout = -1
        }
    }
}
