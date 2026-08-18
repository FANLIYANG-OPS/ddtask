package com.ddtask.scheduler.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.ddtask.scheduler.receiver.ClockInSessionReceiver
import com.ddtask.scheduler.service.GoHomeScheduler

/**
 * 手动打卡会话：打开钉钉后 1 分钟内跟踪打卡成功与返回 DDTask，
 * 并按结果发送成功/失败邮件。无论是否打卡成功，1 分钟后都会回到 DDTask。
 */
class ClockInSessionManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationStorage = NotificationStorage(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())

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
        GoHomeScheduler(appContext).scheduleSessionReturn(sessionId)
    }

    /** 会话仍在 1 分钟窗口内（用于打卡成功/返回关键字匹配）。 */
    fun isActive(): Boolean {
        val start = prefs.getLong(KEY_SESSION_START, 0L)
        if (start <= 0L) return false
        return System.currentTimeMillis() - start < SESSION_TIMEOUT_MS
    }

    /** 会话尚未结束（含 1 分钟超时后、清理前的短暂窗口）。 */
    fun hasOpenSession(): Boolean = prefs.getLong(KEY_SESSION_ID, 0L) > 0L

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

    /** 主界面恢复前台时标记已返回（如定时回退或用户手动切回）。 */
    fun onAppForeground() {
        if (!hasOpenSession()) return
        if (prefs.getBoolean(KEY_RETURNED, false)) return
        val detail = prefs.getString(KEY_PENDING_RETURN_DETAIL, null)
            ?: "已自动返回 DDTask"
        markReturned()
        if (notificationStorage.closeDingTalkEnabled) {
            sendReturnSuccessEmailIfNeeded(detail)
        }
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

        if (!prefs.getBoolean(KEY_RETURNED, false)) {
            performReturn("会话超时（1分钟），自动返回 DDTask")
        }

        mainHandler.postDelayed({
            if (prefs.getLong(KEY_SESSION_ID, 0L) != sessionId) return@postDelayed
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
        }, SESSION_FINALIZE_DELAY_MS)
    }

    private fun performReturn(detail: String) {
        prefs.edit().putString(KEY_PENDING_RETURN_DETAIL, detail).apply()
        AppNavigator.goToMain(appContext)
    }

    private fun markReturned() {
        val sessionId = prefs.getLong(KEY_SESSION_ID, 0L)
        if (sessionId > 0L) {
            GoHomeScheduler(appContext).cancelSessionReturn(sessionId)
        }
        prefs.edit()
            .putBoolean(KEY_RETURNED, true)
            .remove(KEY_PENDING_RETURN_DETAIL)
            .apply()
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
        val sessionId = prefs.getLong(KEY_SESSION_ID, 0L)
        cancelTimeout()
        if (sessionId > 0L) {
            GoHomeScheduler(appContext).cancelSessionReturn(sessionId)
        }
        prefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_SESSION_START)
            .remove(KEY_CLOCK_IN_SUCCESS)
            .remove(KEY_RETURNED)
            .remove(KEY_CLOCK_IN_EMAIL_SENT)
            .remove(KEY_RETURN_EMAIL_SENT)
            .remove(KEY_TRIGGER_SUMMARY)
            .remove(KEY_PENDING_RETURN_DETAIL)
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
        private const val KEY_PENDING_RETURN_DETAIL = "pending_return_detail"
        private const val SESSION_TIMEOUT_MS = 60_000L
        private const val SESSION_FINALIZE_DELAY_MS = 4_000L
        private const val REQUEST_CODE_TIMEOUT = 600_000
    }
}
