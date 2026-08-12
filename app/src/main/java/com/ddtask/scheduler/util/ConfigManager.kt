package com.ddtask.scheduler.util

import android.content.Context
import com.ddtask.scheduler.model.AppConfigExport
import com.ddtask.scheduler.model.ScheduledTask
import com.ddtask.scheduler.service.AlarmScheduler
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

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

        val config = try {
            gson.fromJson(trimmed, AppConfigExport::class.java)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("invalid_json", e)
        } ?: throw IllegalArgumentException("invalid_json")

        if (config.version <= 0 || config.version > AppConfigExport.CURRENT_VERSION) {
            throw IllegalArgumentException("unsupported_version")
        }

        applyConfig(config)
    }

    private fun applyConfig(config: AppConfigExport) {
        taskStorage.getAll().forEach { alarmScheduler.cancel(it.id) }
        taskStorage.saveAll(config.tasks)

        settingsStorage.keepScreenOn = config.keepScreenOn
        settingsStorage.dimScreen = config.dimScreen

        notificationStorage.emailNotifyEnabled = config.emailNotifyEnabled
        notificationStorage.recipientEmail = config.recipientEmail
        notificationStorage.senderEmail = config.senderEmail
        notificationStorage.senderPassword = config.senderPassword
        notificationStorage.smtpHost = config.smtpHost.ifBlank { "smtp.qq.com" }
        notificationStorage.smtpPort = config.smtpPort
        notificationStorage.autoOpenDingTalkEnabled = config.autoOpenDingTalkEnabled
        notificationStorage.triggerKeywords = config.triggerKeywords.ifBlank {
            ClockInDetector.defaultTriggerKeywordsText()
        }
        notificationStorage.successKeywords = config.successKeywords.ifBlank {
            ClockInDetector.defaultSuccessKeywordsText()
        }
        notificationStorage.keywordsConfigured = config.keywordsConfigured ||
            config.triggerKeywords.isNotBlank() ||
            config.successKeywords.isNotBlank()

        alarmScheduler.rescheduleAll()
    }
}
