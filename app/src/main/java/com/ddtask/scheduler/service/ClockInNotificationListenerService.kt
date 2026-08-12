package com.ddtask.scheduler.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ddtask.scheduler.util.ClockInDetector
import com.ddtask.scheduler.util.DingTalkLauncher
import com.ddtask.scheduler.util.EmailSender
import com.ddtask.scheduler.util.NotificationStorage
import com.ddtask.scheduler.util.NotificationTextExtractor

class ClockInNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val storage = NotificationStorage(this)
        val text = NotificationTextExtractor.extract(sbn.notification.extras)
        if (text.isBlank()) return

        handleAutoOpenDingTalk(storage, text)
        handleEmailNotify(storage, sbn.packageName, text)
    }

    private fun handleAutoOpenDingTalk(storage: NotificationStorage, text: String) {
        if (!storage.autoOpenDingTalkEnabled) return
        if (!ClockInDetector.matchesTrigger(text, storage.triggerKeywords)) return
        if (storage.shouldSkipDuplicateOpen()) return
        if (!DingTalkLauncher.launch(this)) return
        storage.recordOpenDingTalk(text)
    }

    private fun handleEmailNotify(storage: NotificationStorage, packageName: String, text: String) {
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
