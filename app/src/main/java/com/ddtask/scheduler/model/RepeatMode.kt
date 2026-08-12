package com.ddtask.scheduler.model

/** 定时任务的重复策略。 */
enum class RepeatMode(val key: String) {
    ONCE("once"),
    DAILY("daily"),
    WEEKDAYS("weekdays"),
    CRON("cron");

    companion object {
        fun fromKey(key: String?): RepeatMode {
            return entries.find { it.key == key } ?: DAILY
        }
    }
}
