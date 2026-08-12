package com.ddtask.scheduler.util

import android.content.Context

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

    fun isConfigured(): Boolean {
        return recipientEmail.isNotBlank() &&
            senderEmail.isNotBlank() &&
            senderPassword.isNotBlank() &&
            smtpHost.isNotBlank()
    }

    fun shouldSkipDuplicate(): Boolean {
        return System.currentTimeMillis() - lastSentAt < DUPLICATE_INTERVAL_MS
    }

    fun recordSent(summary: String) {
        prefs.edit()
            .putLong(KEY_LAST_SENT_AT, System.currentTimeMillis())
            .putString(KEY_LAST_SENT_SUMMARY, summary)
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
        private const val DEFAULT_SMTP_HOST = "smtp.qq.com"
        private const val DEFAULT_SMTP_PORT = 465
        private const val DUPLICATE_INTERVAL_MS = 5 * 60 * 1000L
    }
}
