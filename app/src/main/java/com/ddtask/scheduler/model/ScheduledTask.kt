package com.ddtask.scheduler.model

data class ScheduledTask(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatDaily: Boolean = true,
    val repeatMode: String? = null,
    val cronExpression: String = ""
) {
    fun timeText(): String = String.format("%02d:%02d", hour, minute)

    fun effectiveMode(): RepeatMode {
        if (repeatMode != null) return RepeatMode.fromKey(repeatMode)
        return if (repeatDaily) RepeatMode.DAILY else RepeatMode.ONCE
    }

    fun repeatText(res: android.content.res.Resources): String {
        return when (effectiveMode()) {
            RepeatMode.ONCE -> res.getString(com.ddtask.scheduler.R.string.repeat_once)
            RepeatMode.DAILY -> res.getString(com.ddtask.scheduler.R.string.repeat_daily)
            RepeatMode.WEEKDAYS -> res.getString(com.ddtask.scheduler.R.string.repeat_weekdays)
            RepeatMode.CRON -> {
                val cron = cronExpression.trim()
                if (cron.isEmpty()) {
                    res.getString(com.ddtask.scheduler.R.string.repeat_cron)
                } else {
                    res.getString(com.ddtask.scheduler.R.string.repeat_cron_with_expr, cron)
                }
            }
        }
    }

    fun shouldRescheduleAfterRun(): Boolean = effectiveMode() != RepeatMode.ONCE
}
