package com.ddtask.scheduler.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

/**
 * 组合多种手段保持屏幕常亮：系统熄屏超时、悬浮窗、WakeLock 定期续期。
 */
class ScreenKeepOnController(context: Context) {

    private val appContext = context.applicationContext
    private val settingsStorage = SettingsStorage(appContext)
    private val overlay = KeepScreenOverlay(appContext)
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    private val renewRunnable = object : Runnable {
        override fun run() {
            renewWakeLock()
            handler.postDelayed(this, RENEW_INTERVAL_MS)
        }
    }

    fun start() {
        ScreenTimeoutController.saveAndExtend(appContext, settingsStorage)
        overlay.show()
        acquireWakeLock()
        handler.removeCallbacks(renewRunnable)
        handler.postDelayed(renewRunnable, RENEW_INTERVAL_MS)
    }

    fun stop() {
        handler.removeCallbacks(renewRunnable)
        releaseWakeLock()
        overlay.hide()
        ScreenTimeoutController.restore(appContext, settingsStorage)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            WAKE_LOCK_TAG
        ).apply {
            setReferenceCounted(false)
            acquire(RENEW_INTERVAL_MS + WAKE_LOCK_EXTRA_MS)
        }
    }

    private fun renewWakeLock() {
        releaseWakeLock()
        acquireWakeLock()
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    companion object {
        private const val RENEW_INTERVAL_MS = 5 * TimeConstants.ONE_MINUTE_MS
        private const val WAKE_LOCK_EXTRA_MS = TimeConstants.ONE_MINUTE_MS
        private const val WAKE_LOCK_TAG = "DDTask:KeepScreenOn"
    }
}
