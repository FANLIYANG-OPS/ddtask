package com.ddtask.scheduler.model

data class TaskTemplateEntry(
    val hour: Int,
    val minute: Int,
    val label: String
)

enum class TaskTemplate(val entries: List<TaskTemplateEntry>) {
    SUMMER(
        listOf(
            TaskTemplateEntry(8, 30, "上班"),
            TaskTemplateEntry(18, 0, "下班")
        )
    ),
    WINTER(
        listOf(
            TaskTemplateEntry(8, 30, "上班"),
            TaskTemplateEntry(17, 30, "下班")
        )
    );

    fun createTasks(nextId: () -> Long): List<ScheduledTask> {
        return entries.map { entry ->
            ScheduledTask(
                id = nextId(),
                hour = entry.hour,
                minute = entry.minute,
                label = entry.label,
                enabled = true,
                repeatDaily = false,
                repeatMode = RepeatMode.WEEKDAYS.key,
                cronExpression = ""
            )
        }
    }
}
