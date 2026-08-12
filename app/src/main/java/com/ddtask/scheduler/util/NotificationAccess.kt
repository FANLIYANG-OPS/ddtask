package com.ddtask.scheduler.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.ddtask.scheduler.service.ClockInNotificationListenerService

/** 检查本应用的通知监听服务是否已在系统设置中授权。 */
object NotificationAccess {

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val component = ComponentName(context, ClockInNotificationListenerService::class.java)
        return enabled.split(':').any { it.equals(component.flattenToString(), ignoreCase = true) }
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
