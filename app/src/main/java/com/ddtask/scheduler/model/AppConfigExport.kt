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
    val triggerKeywords: String = "",
    val successKeywords: String = "",
    val keywordsConfigured: Boolean = false
) {
    companion object {
        /** Unchanged so v1.12.0 exports/imports remain compatible. */
        const val CURRENT_VERSION = 1
    }
}
