package com.ddtask.scheduler.util

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.service.ReturnToAppService
import com.ddtask.scheduler.service.ScreenControlService

/** 将 DDTask 主界面带到前台（兼容 Android 6.0+ 后台启动限制）。 */
object AppNavigator {

    fun goToMain(context: Context) {
        val appContext = context.applicationContext
        stopScreenControl(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        // Android 6.0–9 后台启动限制较松，优先直接 startActivity
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (tryDirectLaunch(appContext, intent)) return
        }
        if (tryPendingIntentLaunch(appContext, intent)) return
        if (tryDirectLaunch(appContext, intent)) return
        tryForegroundServiceLaunch(appContext)
    }

    private fun stopScreenControl(context: Context) {
        context.startService(
            Intent(context, ScreenControlService::class.java).apply {
                action = ScreenControlService.ACTION_STOP
            }
        )
    }

    private fun tryPendingIntentLaunch(context: Context, intent: Intent): Boolean {
        return try {
            val pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_RETURN,
                intent,
                PendingIntentCompat.updateCurrentImmutable()
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                pendingIntent.send(
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    options.toBundle()
                )
            } else {
                pendingIntent.send()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun tryDirectLaunch(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun tryForegroundServiceLaunch(context: Context) {
        val serviceIntent = Intent(context, ReturnToAppService::class.java).apply {
            action = ReturnToAppService.ACTION_RETURN
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            // Android 6.0/7.x：普通 startService 即可拉起界面
            context.startService(serviceIntent)
        }
    }

    private const val REQUEST_RETURN = 300_001
}
