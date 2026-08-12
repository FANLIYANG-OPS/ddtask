package com.ddtask.scheduler.util

import android.content.Context
import com.ddtask.scheduler.model.AppConfigExport
import com.ddtask.scheduler.service.AlarmScheduler
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

/** 将任务、屏幕、通知/邮件/关键字等配置导出为 JSON，并支持导入（兼容 v1.12.0）。 */
class ConfigManager(private val context: Context) {

    private val gson = Gson()
    private val taskStorage = TaskStorage(context)
    private val settingsStorage = SettingsStorage(context)
    private val notificationStorage = NotificationStorage(context)
    private val alarmScheduler = AlarmScheduler(context)

    fun exportJson(): String {
        val config = AppConfigExport(
            version = AppConfigExport.CURRENT_VERSION,
            exportedAt = System.currentTimeMillis(),
            tasks = taskStorage.getAll(),
            keepScreenOn = settingsStorage.keepScreenOn,
            dimScreen = settingsStorage.dimScreen,
            emailNotifyEnabled = notificationStorage.emailNotifyEnabled,
            recipientEmail = notificationStorage.recipientEmail,
            senderEmail = notificationStorage.senderEmail,
            senderPassword = notificationStorage.senderPassword,
            smtpHost = notificationStorage.smtpHost,
            smtpPort = notificationStorage.smtpPort,
            autoOpenDingTalkEnabled = notificationStorage.autoOpenDingTalkEnabled,
            triggerKeywords = notificationStorage.triggerKeywords,
            successKeywords = notificationStorage.successKeywords,
            keywordsConfigured = notificationStorage.keywordsConfigured
        )
        return gson.toJson(config)
    }

    fun importJson(json: String) {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("empty_json")
        }

        val root = try {
            JsonParser.parseString(trimmed).asJsonObject
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("invalid_json", e)
        }

        val version = root.get("version")?.takeIf { it.isJsonPrimitive }?.asInt ?: 1
        if (version <= 0 || version > AppConfigExport.CURRENT_VERSION) {
            throw IllegalArgumentException("unsupported_version")
        }

        val config = try {
            gson.fromJson(trimmed, AppConfigExport::class.java)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("invalid_json", e)
        } ?: throw IllegalArgumentException("invalid_json")

        val keywordsConfigured = resolveKeywordsConfigured(root, config)
        applyConfig(config, keywordsConfigured)
    }

    /**
     * v1.12.0 exports omit [keywordsConfigured]; infer from JSON keys for backward compatibility.
     */
    private fun resolveKeywordsConfigured(
        root: com.google.gson.JsonObject,
        config: AppConfigExport
    ): Boolean {
        if (root.has("keywordsConfigured")) {
            return config.keywordsConfigured
        }
        val hasTriggerKey = root.has("triggerKeywords")
        val hasSuccessKey = root.has("successKeywords")
        if (!hasTriggerKey && !hasSuccessKey) {
            return false
        }
        return config.triggerKeywords.isNotBlank() || config.successKeywords.isNotBlank()
    }

    /** 写入各存储并重建全部闹钟（导入会先取消旧任务）。 */
    private fun applyConfig(config: AppConfigExport, keywordsConfigured: Boolean) {
        taskStorage.getAll().forEach { alarmScheduler.cancel(it.id) }
        taskStorage.saveAll(config.tasks.orEmpty())

        settingsStorage.keepScreenOn = config.keepScreenOn
        settingsStorage.dimScreen = config.dimScreen

        notificationStorage.emailNotifyEnabled = config.emailNotifyEnabled
        notificationStorage.recipientEmail = config.recipientEmail
        notificationStorage.senderEmail = config.senderEmail
        notificationStorage.senderPassword = config.senderPassword
        notificationStorage.smtpHost = config.smtpHost.ifBlank { "smtp.qq.com" }
        notificationStorage.smtpPort = config.smtpPort.takeIf { it in 1..65535 } ?: 465
        notificationStorage.autoOpenDingTalkEnabled = config.autoOpenDingTalkEnabled
        notificationStorage.triggerKeywords = config.triggerKeywords.ifBlank {
            ClockInDetector.defaultTriggerKeywordsText()
        }
        notificationStorage.successKeywords = config.successKeywords.ifBlank {
            ClockInDetector.defaultSuccessKeywordsText()
        }
        notificationStorage.keywordsConfigured = keywordsConfigured

        alarmScheduler.rescheduleAll()
    }

    private fun List<com.ddtask.scheduler.model.ScheduledTask>?.orEmpty() = this ?: emptyList()
}
