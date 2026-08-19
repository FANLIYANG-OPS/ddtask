package com.ddtask.scheduler.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ddtask.scheduler.util.ClockInActionHandler
import com.ddtask.scheduler.util.ClockInSource
import com.ddtask.scheduler.util.NotificationTextExtractor

/**
 * 系统通知监听服务。
 * 收到通知后提取文本，按用户配置的关键字触发「自动打开钉钉」或「打卡成功发邮件」。
 */
class ClockInNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val text = NotificationTextExtractor.extract(sbn.notification.extras)
        ClockInActionHandler(this).handleIncomingText(
            text,
            ClockInSource.NOTIFICATION,
            sbn.packageName
        )
    }
}
