package com.ddtask.scheduler.util

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * 1x1 透明悬浮窗，持有 FLAG_KEEP_SCREEN_ON。
 * 在其它 App（如钉钉）处于前台时也能阻止熄屏。
 */
class KeepScreenOverlay(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    fun canDrawOverlay(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun show() {
        if (!canDrawOverlay() || overlayView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = View(context)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            OVERLAY_WIDTH_PX,
            OVERLAY_HEIGHT_PX,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = OVERLAY_POSITION
            y = OVERLAY_POSITION
        }

        windowManager?.addView(overlayView, params)
    }

    fun hide() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        overlayView = null
        windowManager = null
    }

    companion object {
        private const val OVERLAY_WIDTH_PX = 1
        private const val OVERLAY_HEIGHT_PX = 1
        private const val OVERLAY_POSITION = 0
    }
}
