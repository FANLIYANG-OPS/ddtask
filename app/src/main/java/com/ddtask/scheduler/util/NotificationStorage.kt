package com.ddtask.scheduler.util

import android.content.Context

/** 通知监听、邮件 SMTP、关键字及最近触发记录的 SharedPreferences 封装。 */
class NotificationStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var emailNotifyEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var recipientEmail: String
        get() = prefs.getString(KEY_RECIPIENT, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_RECIPIENT, value).apply()

    var senderEmail: String
        get() = prefs.getString(KEY_SENDER, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SENDER, value).apply()

    var senderPassword: String
        get() = prefs.getString(KEY_PASSWORD, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    var smtpHost: String
        get() = prefs.getString(KEY_SMTP_HOST, DEFAULT_SMTP_HOST).orEmpty()
        set(value) = prefs.edit().putString(KEY_SMTP_HOST, value).apply()

    var smtpPort: Int
        get() = prefs.getInt(KEY_SMTP_PORT, DEFAULT_SMTP_PORT)
        set(value) = prefs.edit().putInt(KEY_SMTP_PORT, value).apply()

    var lastSentAt: Long
        get() = prefs.getLong(KEY_LAST_SENT_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SENT_AT, value).apply()

    var lastSentSummary: String
        get() = prefs.getString(KEY_LAST_SENT_SUMMARY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_SENT_SUMMARY, value).apply()

    var autoOpenDingTalkEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_OPEN_DINGTALK, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_OPEN_DINGTALK, value).apply()

    var triggerKeywords: String
        get() = prefs.getString(KEY_TRIGGER_KEYWORDS, ClockInDetector.defaultTriggerKeywordsText()).orEmpty()
        set(value) = prefs.edit().putString(KEY_TRIGGER_KEYWORDS, value).apply()

    var successKeywords: String
        get() = prefs.getString(KEY_SUCCESS_KEYWORDS, ClockInDetector.defaultSuccessKeywordsText()).orEmpty()
        set(value) = prefs.edit().putString(KEY_SUCCESS_KEYWORDS, value).apply()

    var keywordsConfigured: Boolean
        get() = prefs.getBoolean(KEY_KEYWORDS_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_KEYWORDS_CONFIGURED, value).apply()

    var lastOpenDingTalkAt: Long
        get() = prefs.getLong(KEY_LAST_OPEN_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_OPEN_AT, value).apply()

    var lastOpenDingTalkSummary: String
        get() = prefs.getString(KEY_LAST_OPEN_SUMMARY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_OPEN_SUMMARY, value).apply()

    fun isConfigured(): Boolean {
        return recipientEmail.isNotBlank() &&
            senderEmail.isNotBlank() &&
            senderPassword.isNotBlank() &&
            smtpHost.isNotBlank()
    }

    /** 5 分钟内不重复发邮件，避免同一条通知多次触发。 */
    fun shouldSkipDuplicateEmail(): Boolean {
        return System.currentTimeMillis() - lastSentAt < DUPLICATE_INTERVAL_MS
    }

    /** 1 分钟内不重复打开钉钉，避免通知风暴。 */
    fun shouldSkipDuplicateOpen(): Boolean {
        return System.currentTimeMillis() - lastOpenDingTalkAt < OPEN_DINGTALK_INTERVAL_MS
    }

    fun recordEmailSent(summary: String) {
        prefs.edit()
            .putLong(KEY_LAST_SENT_AT, System.currentTimeMillis())
            .putString(KEY_LAST_SENT_SUMMARY, summary)
            .apply()
    }

    fun recordOpenDingTalk(summary: String) {
        prefs.edit()
            .putLong(KEY_LAST_OPEN_AT, System.currentTimeMillis())
            .putString(KEY_LAST_OPEN_SUMMARY, summary)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "ddtask_notification"
        private const val KEY_ENABLED = "email_notify_enabled"
        private const val KEY_RECIPIENT = "recipient_email"
        private const val KEY_SENDER = "sender_email"
        private const val KEY_PASSWORD = "sender_password"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_LAST_SENT_AT = "last_sent_at"
        private const val KEY_LAST_SENT_SUMMARY = "last_sent_summary"
        private const val KEY_AUTO_OPEN_DINGTALK = "auto_open_dingtalk"
        private const val KEY_TRIGGER_KEYWORDS = "trigger_keywords"
        private const val KEY_SUCCESS_KEYWORDS = "success_keywords"
        private const val KEY_KEYWORDS_CONFIGURED = "keywords_configured"
        private const val KEY_LAST_OPEN_AT = "last_open_at"
        private const val KEY_LAST_OPEN_SUMMARY = "last_open_summary"
        private const val DEFAULT_SMTP_HOST = "smtp.qq.com"
        private const val DEFAULT_SMTP_PORT = 465
        private const val DUPLICATE_INTERVAL_MS = 5 * 60 * 1000L
        private const val OPEN_DINGTALK_INTERVAL_MS = 60 * 1000L
    }
}
