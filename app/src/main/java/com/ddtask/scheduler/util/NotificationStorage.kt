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

    var returnKeywords: String
        get() = prefs.getString(KEY_RETURN_KEYWORDS, ClockInDetector.defaultReturnKeywordsText()).orEmpty()
        set(value) = prefs.edit().putString(KEY_RETURN_KEYWORDS, value).apply()

    var closeDingTalkEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOSE_DINGTALK, false)
        set(value) = prefs.edit().putBoolean(KEY_CLOSE_DINGTALK, value).apply()

    /** 邮件触发：轮询发件邮箱收件箱，复用下方 SMTP/发件箱配置，无需额外设置。 */
    var emailTriggerEnabled: Boolean
        get() = prefs.getBoolean(KEY_EMAIL_TRIGGER, false)
        set(value) = prefs.edit().putBoolean(KEY_EMAIL_TRIGGER, value).apply()

    /** 由 SMTP 地址自动推断 IMAP 服务器（如 smtp.qq.com → imap.qq.com）。 */
    fun resolvedImapHost(): String = ImapHelper.resolveHost(smtpHost)

    fun resolvedImapPort(): Int = ImapHelper.defaultPort()

    var lastProcessedImapUid: Long
        get() = prefs.getLong(KEY_LAST_IMAP_UID, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_IMAP_UID, value).apply()

    var lastEmailTriggerAt: Long
        get() = prefs.getLong(KEY_LAST_EMAIL_TRIGGER_AT, 0L)
        private set(value) = prefs.edit().putLong(KEY_LAST_EMAIL_TRIGGER_AT, value).apply()

    var lastEmailTriggerSummary: String
        get() = prefs.getString(KEY_LAST_EMAIL_TRIGGER_SUMMARY, "").orEmpty()
        private set(value) = prefs.edit().putString(KEY_LAST_EMAIL_TRIGGER_SUMMARY, value).apply()

    var keywordsConfigured: Boolean
        get() = prefs.getBoolean(KEY_KEYWORDS_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_KEYWORDS_CONFIGURED, value).apply()

    var lastOpenDingTalkAt: Long
        get() = prefs.getLong(KEY_LAST_OPEN_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_OPEN_AT, value).apply()

    var lastOpenDingTalkSummary: String
        get() = prefs.getString(KEY_LAST_OPEN_SUMMARY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LAST_OPEN_SUMMARY, value).apply()

    private var lastReturnAt: Long
        get() = prefs.getLong(KEY_LAST_RETURN_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_RETURN_AT, value).apply()

    fun isConfigured(): Boolean {
        return recipientEmail.isNotBlank() &&
            senderEmail.isNotBlank() &&
            senderPassword.isNotBlank() &&
            smtpHost.isNotBlank()
    }

    /** 发件邮箱是否可用于 IMAP 收信（与发通知共用同一套账号密码）。 */
    fun isSenderMailboxReady(): Boolean {
        return senderEmail.isNotBlank() &&
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

    /** 手动打开钉钉后 5 分钟内可响应返回关键字（即使 1 分钟会话已结束）。 */
    fun isWithinManualReturnWindow(): Boolean {
        val lastOpen = lastOpenDingTalkAt
        if (lastOpen <= 0L) return false
        return System.currentTimeMillis() - lastOpen < MANUAL_RETURN_WINDOW_MS
    }

    /** 本次打开钉钉是否已完成返回。 */
    var returnedForCurrentOpen: Boolean
        get() = prefs.getBoolean(KEY_RETURNED_FOR_OPEN, false)
        private set(value) = prefs.edit().putBoolean(KEY_RETURNED_FOR_OPEN, value).apply()

    fun canAcceptReturnKeyword(): Boolean {
        return isWithinManualReturnWindow() && !returnedForCurrentOpen
    }

    fun markReturnedForCurrentOpen() {
        returnedForCurrentOpen = true
    }

    /** 2 秒内不重复触发返回，避免同一条通知多次匹配。 */
    fun shouldSkipDuplicateReturn(): Boolean {
        return System.currentTimeMillis() - lastReturnAt < RETURN_DEBOUNCE_MS
    }

    fun recordReturnTriggered() {
        lastReturnAt = System.currentTimeMillis()
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
            .putBoolean(KEY_RETURNED_FOR_OPEN, false)
            .apply()
    }

    fun recordEmailTrigger(summary: String) {
        lastEmailTriggerAt = System.currentTimeMillis()
        lastEmailTriggerSummary = summary
    }

    var lastImapPollAt: Long
        get() = prefs.getLong(KEY_LAST_IMAP_POLL_AT, 0L)
        private set(value) = prefs.edit().putLong(KEY_LAST_IMAP_POLL_AT, value).apply()

    var lastImapPollError: String
        get() = prefs.getString(KEY_LAST_IMAP_POLL_ERROR, "").orEmpty()
        private set(value) = prefs.edit().putString(KEY_LAST_IMAP_POLL_ERROR, value).apply()

    fun recordImapPoll(error: String?) {
        lastImapPollAt = System.currentTimeMillis()
        lastImapPollError = error.orEmpty()
    }

    fun resetImapCursor() {
        lastProcessedImapUid = 0L
        lastImapPollError = ""
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
        private const val KEY_RETURN_KEYWORDS = "return_keywords"
        private const val KEY_CLOSE_DINGTALK = "close_dingtalk_enabled"
        private const val KEY_EMAIL_TRIGGER = "email_trigger_enabled"
        private const val KEY_LAST_IMAP_UID = "last_imap_uid"
        private const val KEY_LAST_EMAIL_TRIGGER_AT = "last_email_trigger_at"
        private const val KEY_LAST_EMAIL_TRIGGER_SUMMARY = "last_email_trigger_summary"
        private const val KEY_LAST_IMAP_POLL_AT = "last_imap_poll_at"
        private const val KEY_LAST_IMAP_POLL_ERROR = "last_imap_poll_error"
        private const val KEY_KEYWORDS_CONFIGURED = "keywords_configured"
        private const val KEY_LAST_OPEN_AT = "last_open_at"
        private const val KEY_LAST_OPEN_SUMMARY = "last_open_summary"
        private const val DEFAULT_SMTP_HOST = "smtp.qq.com"
        private const val DEFAULT_SMTP_PORT = 465
        private const val DUPLICATE_INTERVAL_MS = 5 * 60 * 1000L
        private const val OPEN_DINGTALK_INTERVAL_MS = 60 * 1000L
        private const val MANUAL_RETURN_WINDOW_MS = 5 * 60 * 1000L
        private const val RETURN_DEBOUNCE_MS = 2 * 1000L
        private const val KEY_LAST_RETURN_AT = "last_return_at"
        private const val KEY_RETURNED_FOR_OPEN = "returned_for_open"
    }
}
