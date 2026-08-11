package com.ddtask.scheduler.model

data class ScheduledTask(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatDaily: Boolean = true
) {
    fun timeText(): String = String.format("%02d:%02d", hour, minute)
}
