package com.ddtask.scheduler.model

/** 导出/导入 JSON 的数据结构；[CURRENT_VERSION] 保持为 1 以兼容 v1.12.0。 */
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
    val closeDingTalkEnabled: Boolean = false,
    val triggerKeywords: String = "",
    val successKeywords: String = "",
    val returnKeywords: String = "",
    val keywordsConfigured: Boolean = false,
    val emailTriggerEnabled: Boolean = false,
    val imapHost: String = "",
    val imapPort: Int = 993
) {
    companion object {
        /** v3 起增加邮件触发打卡。 */
        const val CURRENT_VERSION = 3
    }
}
