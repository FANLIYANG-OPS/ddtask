package com.ddtask.scheduler.model

import com.ddtask.scheduler.util.NewTaskSpec

data class TaskTemplateEntry(
    val hour: Int,
    val minute: Int,
    val label: String
)

/** 夏/冬季上下班预设：仅提供数据，创建时生成两条独立任务。 */
enum class TaskTemplate(val entries: List<TaskTemplateEntry>) {
    SUMMER(
        listOf(
            TaskTemplateEntry(8, 0, "上班"),
            TaskTemplateEntry(18, 0, "下班")
        )
    ),
    WINTER(
        listOf(
            TaskTemplateEntry(8, 0, "上班"),
            TaskTemplateEntry(17, 30, "下班")
        )
    );

    fun toTaskSpecs(): List<NewTaskSpec> {
        return entries.map { entry ->
            NewTaskSpec(
                hour = entry.hour,
                minute = entry.minute,
                label = entry.label,
                enabled = true,
                repeatMode = RepeatMode.WEEKDAYS
            )
        }
    }
}
