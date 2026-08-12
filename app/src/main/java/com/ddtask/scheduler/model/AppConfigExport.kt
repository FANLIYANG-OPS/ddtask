package com.ddtask.scheduler.model

data class AppConfigExport(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val tasks: List<ScheduledTask> = emptyList(),
    val keepScreenOn: Boolean = false,
    val dimScreen: Boolean = false,
    val emailNotifyEnabled: Boolean = false,
    val recipientEmail: String = "",
    val senderEmail: String = "",
    val senderPassword: String = "",
    val smtpHost: String = "",
    val smtpPort: Int = 465,
    val autoOpenDingTalkEnabled: Boolean = false,
    val triggerKeywords: String = "",
    val successKeywords: String = ""
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
