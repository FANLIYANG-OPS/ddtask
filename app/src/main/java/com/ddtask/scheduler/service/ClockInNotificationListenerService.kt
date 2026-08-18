package com.ddtask.scheduler.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ddtask.scheduler.util.ClockInDetector
import com.ddtask.scheduler.util.ClockInSessionManager
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
        handleReturnToApp(storage, text)
        handleClockInSuccess(storage, sbn.packageName, text)
    }

    private fun handleAutoOpenDingTalk(storage: NotificationStorage, text: String) {
        if (!storage.autoOpenDingTalkEnabled) return
        if (!ClockInDetector.matchesTrigger(text, storage.triggerKeywords)) return
        if (storage.shouldSkipDuplicateOpen()) return
        if (!DingTalkLauncher.launch(this)) return
        storage.recordOpenDingTalk(text)
        if (storage.emailNotifyEnabled || storage.closeDingTalkEnabled) {
            ClockInSessionManager(this).startSession(text)
        }
    }

    private fun handleReturnToApp(storage: NotificationStorage, text: String) {
        if (!storage.closeDingTalkEnabled) return
        if (!ClockInDetector.matchesReturn(text, storage.returnKeywords, storage.successKeywords)) return
        ClockInSessionManager(this).onReturnKeywordMatched(text)
    }

    private fun handleClockInSuccess(storage: NotificationStorage, packageName: String, text: String) {
        if (packageName != ClockInDetector.DINGTALK_PACKAGE) return
        if (!ClockInDetector.matchesSuccess(text, storage.successKeywords)) return

        val sessionManager = ClockInSessionManager(this)
        if (sessionManager.isActive()) {
            sessionManager.onClockInSuccess(text)
            return
        }

        // 无活跃会话时保持原有即时邮件行为
        if (!storage.emailNotifyEnabled || !storage.isConfigured()) return
        if (storage.shouldSkipDuplicateEmail()) return
        EmailSender.sendClockInSuccess(this, text) { success, _ ->
            if (success) storage.recordEmailSent(text)
        }
    }
}
