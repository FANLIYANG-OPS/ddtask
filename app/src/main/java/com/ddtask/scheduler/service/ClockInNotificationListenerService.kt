package com.ddtask.scheduler.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ddtask.scheduler.util.ClockInDetector
import com.ddtask.scheduler.util.DingTalkLauncher
import com.ddtask.scheduler.util.EmailSender
import com.ddtask.scheduler.util.NotificationStorage
import com.ddtask.scheduler.util.NotificationTextExtractor

/**
 * 系统通知监听服务。
 * 收到通知后提取文本，按用户配置的关键字触发「自动打开钉钉」或「打卡成功发邮件」。
 */
class ClockInNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val storage = NotificationStorage(this)
        val text = NotificationTextExtractor.extract(sbn.notification.extras)
        if (text.isBlank()) return

        handleAutoOpenDingTalk(storage, text)
        handleEmailNotify(storage, sbn.packageName, text)
    }

    private fun handleAutoOpenDingTalk(storage: NotificationStorage, text: String) {
        // 任意应用通知均可触发，不限于钉钉
        if (!storage.autoOpenDingTalkEnabled) return
        if (!ClockInDetector.matchesTrigger(text, storage.triggerKeywords)) return
        if (storage.shouldSkipDuplicateOpen()) return
        if (!DingTalkLauncher.launch(this)) return
        storage.recordOpenDingTalk(text)
    }

    private fun handleEmailNotify(storage: NotificationStorage, packageName: String, text: String) {
        // 仅处理钉钉发出的打卡成功通知
        if (packageName != ClockInDetector.DINGTALK_PACKAGE) return
        if (!storage.emailNotifyEnabled || !storage.isConfigured()) return
        if (!ClockInDetector.matchesSuccess(text, storage.successKeywords)) return
        if (storage.shouldSkipDuplicateEmail()) return

        EmailSender.sendClockInSuccess(this, text) { success, _ ->
            if (success) {
                storage.recordEmailSent(text)
            }
        }
    }
}
