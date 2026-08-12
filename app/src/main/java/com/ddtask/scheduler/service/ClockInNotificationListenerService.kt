package com.ddtask.scheduler.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ddtask.scheduler.util.ClockInDetector
import com.ddtask.scheduler.util.EmailSender
import com.ddtask.scheduler.util.NotificationStorage

class ClockInNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != ClockInDetector.DINGTALK_PACKAGE) return

        val storage = NotificationStorage(this)
        if (!storage.emailNotifyEnabled || !storage.isConfigured()) return

        val text = extractNotificationText(sbn.notification.extras)
        if (!ClockInDetector.isClockInSuccess(text)) return
        if (storage.shouldSkipDuplicate()) return

        EmailSender.sendClockInSuccess(this, text) { success, _ ->
            if (success) {
                storage.recordSent(text)
            }
        }
    }

    private fun extractNotificationText(extras: android.os.Bundle): String {
        val parts = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach {
            parts.add(it.toString())
        }
        return parts.joinToString(" ")
    }
}
