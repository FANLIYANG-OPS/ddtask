package com.ddtask.scheduler.util

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.ReturnProxyActivity
import com.ddtask.scheduler.service.ReturnToAppService
import com.ddtask.scheduler.service.ScreenControlService

/** 将 DDTask 主界面带到前台（兼容 Android 6.0+ 后台启动限制）。 */
object AppNavigator {

    fun goToMain(context: Context) {
        val appContext = context.applicationContext
        stopScreenControl(appContext)
        val intent = mainIntent(appContext)

        if (context is Activity) {
            if (tryDirectLaunch(appContext, intent)) return
            tryProxyLaunch(appContext)
            return
        }

        // 通知监听、广播等后台上下文：透明 Activity 中转在 Android 6.0 上最可靠
        if (tryProxyLaunch(appContext)) return
        if (tryForegroundServiceLaunch(appContext)) return
        if (tryPendingIntentLaunch(appContext, intent)) return
        tryDirectLaunch(appContext, intent)
    }

    private fun mainIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
    }

    private fun stopScreenControl(context: Context) {
        context.startService(
            Intent(context, ScreenControlService::class.java).apply {
                action = ScreenControlService.ACTION_STOP
            }
        )
    }

    private fun tryProxyLaunch(context: Context): Boolean {
        return try {
            context.startActivity(
                Intent(context, ReturnProxyActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                }
            )
            true
        } catch (_: Exception) {
            false
        }
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

    private fun tryForegroundServiceLaunch(context: Context): Boolean {
        return try {
            val serviceIntent = Intent(context, ReturnToAppService::class.java).apply {
                action = ReturnToAppService.ACTION_RETURN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private const val REQUEST_RETURN = 300_001
}
