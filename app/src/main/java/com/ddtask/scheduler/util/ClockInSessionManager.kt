package com.ddtask.scheduler.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.receiver.ClockInSessionReceiver
import com.ddtask.scheduler.util.PendingIntentCompat

/**
 * 手动打卡会话：打开钉钉后 1 分钟内跟踪打卡成功与返回 DDTask，
 * 并按结果发送成功/失败邮件。
 */
class ClockInSessionManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationStorage = NotificationStorage(appContext)

    fun startSession(triggerSummary: String) {
        val sessionId = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_SESSION_ID, sessionId)
            .putLong(KEY_SESSION_START, sessionId)
            .putBoolean(KEY_CLOCK_IN_SUCCESS, false)
            .putBoolean(KEY_RETURNED, false)
            .putBoolean(KEY_CLOCK_IN_EMAIL_SENT, false)
            .putBoolean(KEY_RETURN_EMAIL_SENT, false)
            .putString(KEY_TRIGGER_SUMMARY, triggerSummary)
            .apply()
        scheduleTimeout(sessionId)
    }

    fun isActive(): Boolean {
        val start = prefs.getLong(KEY_SESSION_START, 0L)
        if (start <= 0L) return false
        return System.currentTimeMillis() - start < SESSION_TIMEOUT_MS
    }

    fun onClockInSuccess(notificationText: String) {
        if (!isActive() || prefs.getBoolean(KEY_CLOCK_IN_SUCCESS, false)) return
        prefs.edit().putBoolean(KEY_CLOCK_IN_SUCCESS, true).apply()
        sendClockInSuccessEmailIfNeeded(notificationText)
        if (notificationStorage.closeDingTalkEnabled && !prefs.getBoolean(KEY_RETURNED, false)) {
            performReturn(notificationText)
        }
    }

    /** 匹配返回关键字时回到 DDTask 并记录返回成功。 */
    fun onReturnKeywordMatched(notificationText: String) {
        if (!isActive() || !notificationStorage.closeDingTalkEnabled) return
        if (prefs.getBoolean(KEY_RETURNED, false)) return
        performReturn(notificationText)
    }

    /** 主界面恢复前台时标记已返回（如 GoHome 或用户手动切回）。 */
    fun onAppForeground() {
        if (!isActive() || !notificationStorage.closeDingTalkEnabled) return
        if (prefs.getBoolean(KEY_RETURNED, false)) return
        markReturned()
        sendReturnSuccessEmailIfNeeded("已自动返回 DDTask")
    }

    fun onTimeout(sessionId: Long) {
        if (prefs.getLong(KEY_SESSION_ID, 0L) != sessionId) return

        val triggerSummary = prefs.getString(KEY_TRIGGER_SUMMARY, "").orEmpty()
        if (notificationStorage.emailNotifyEnabled && notificationStorage.isConfigured()) {
            if (!prefs.getBoolean(KEY_CLOCK_IN_SUCCESS, false) &&
                !prefs.getBoolean(KEY_CLOCK_IN_EMAIL_SENT, false)
            ) {
                prefs.edit().putBoolean(KEY_CLOCK_IN_EMAIL_SENT, true).apply()
                EmailSender.sendClockInFailure(appContext, triggerSummary) { success, _ ->
                    if (success) notificationStorage.recordEmailSent("打卡失败（超时）")
                }
            }
        }

        if (notificationStorage.closeDingTalkEnabled &&
            notificationStorage.isConfigured() &&
            !prefs.getBoolean(KEY_RETURNED, false) &&
            !prefs.getBoolean(KEY_RETURN_EMAIL_SENT, false)
        ) {
            prefs.edit().putBoolean(KEY_RETURN_EMAIL_SENT, true).apply()
            EmailSender.sendReturnFailure(appContext, triggerSummary) { success, _ ->
                if (success) notificationStorage.recordEmailSent("返回 DDTask 失败（超时）")
            }
        }

        clearSession()
    }

    private fun performReturn(detail: String) {
        AppNavigator.goToMain(appContext)
        markReturned()
        sendReturnSuccessEmailIfNeeded(detail)
    }

    private fun markReturned() {
        prefs.edit().putBoolean(KEY_RETURNED, true).apply()
    }

    private fun sendClockInSuccessEmailIfNeeded(notificationText: String) {
        if (!notificationStorage.emailNotifyEnabled || !notificationStorage.isConfigured()) return
        if (prefs.getBoolean(KEY_CLOCK_IN_EMAIL_SENT, false)) return
        prefs.edit().putBoolean(KEY_CLOCK_IN_EMAIL_SENT, true).apply()
        EmailSender.sendClockInSuccess(appContext, notificationText) { success, _ ->
            if (success) notificationStorage.recordEmailSent(notificationText)
        }
    }

    private fun sendReturnSuccessEmailIfNeeded(detail: String) {
        if (!notificationStorage.closeDingTalkEnabled || !notificationStorage.isConfigured()) return
        if (prefs.getBoolean(KEY_RETURN_EMAIL_SENT, false)) return
        prefs.edit().putBoolean(KEY_RETURN_EMAIL_SENT, true).apply()
        EmailSender.sendReturnSuccess(appContext, detail) { success, _ ->
            if (success) notificationStorage.recordEmailSent("已返回 DDTask")
        }
    }

    private fun scheduleTimeout(sessionId: Long) {
        cancelTimeout()
        val triggerAt = sessionId + SESSION_TIMEOUT_MS
        val pendingIntent = createTimeoutPendingIntent(sessionId)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            // 无精确闹钟权限时仍保留会话，仅依赖内存超时判断
        }
    }

    private fun cancelTimeout() {
        val sessionId = prefs.getLong(KEY_SESSION_ID, 0L)
        if (sessionId > 0L) {
            alarmManager.cancel(createTimeoutPendingIntent(sessionId))
        }
    }

    private fun createTimeoutPendingIntent(sessionId: Long): PendingIntent {
        val intent = Intent(appContext, ClockInSessionReceiver::class.java).apply {
            action = ClockInSessionReceiver.ACTION_SESSION_TIMEOUT
            putExtra(ClockInSessionReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val flags = PendingIntentCompat.updateCurrentImmutable()
        return PendingIntent.getBroadcast(appContext, REQUEST_CODE_TIMEOUT, intent, flags)
    }

    private fun clearSession() {
        cancelTimeout()
        prefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_SESSION_START)
            .remove(KEY_CLOCK_IN_SUCCESS)
            .remove(KEY_RETURNED)
            .remove(KEY_CLOCK_IN_EMAIL_SENT)
            .remove(KEY_RETURN_EMAIL_SENT)
            .remove(KEY_TRIGGER_SUMMARY)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "ddtask_clock_in_session"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_SESSION_START = "session_start"
        private const val KEY_CLOCK_IN_SUCCESS = "clock_in_success"
        private const val KEY_RETURNED = "returned"
        private const val KEY_CLOCK_IN_EMAIL_SENT = "clock_in_email_sent"
        private const val KEY_RETURN_EMAIL_SENT = "return_email_sent"
        private const val KEY_TRIGGER_SUMMARY = "trigger_summary"
        private const val SESSION_TIMEOUT_MS = 60_000L
        private const val REQUEST_CODE_TIMEOUT = 600_000
    }
}
