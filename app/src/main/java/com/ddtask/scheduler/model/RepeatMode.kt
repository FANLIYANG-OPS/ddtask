package com.ddtask.scheduler.model

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
