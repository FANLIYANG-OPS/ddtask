package com.ddtask.scheduler.model

import com.ddtask.scheduler.util.NewTaskSpec

private object TaskTemplateDefaults {
    const val WORK_START_HOUR = 8
    const val WORK_START_MINUTE = 0
    const val SUMMER_END_HOUR = 18
    const val SUMMER_END_MINUTE = 0
    const val WINTER_END_HOUR = 17
    const val WINTER_END_MINUTE = 30
    const val LABEL_WORK_START = "上班"
    const val LABEL_WORK_END = "下班"
}

data class TaskTemplateEntry(
    val hour: Int,
    val minute: Int,
    val label: String
)

/** 夏/冬季上下班预设：仅提供数据，创建时生成两条独立任务。 */
enum class TaskTemplate(val entries: List<TaskTemplateEntry>) {
    SUMMER(
        listOf(
            TaskTemplateEntry(
                TaskTemplateDefaults.WORK_START_HOUR,
                TaskTemplateDefaults.WORK_START_MINUTE,
                TaskTemplateDefaults.LABEL_WORK_START
            ),
            TaskTemplateEntry(
                TaskTemplateDefaults.SUMMER_END_HOUR,
                TaskTemplateDefaults.SUMMER_END_MINUTE,
                TaskTemplateDefaults.LABEL_WORK_END
            )
        )
    ),
    WINTER(
        listOf(
            TaskTemplateEntry(
                TaskTemplateDefaults.WORK_START_HOUR,
                TaskTemplateDefaults.WORK_START_MINUTE,
                TaskTemplateDefaults.LABEL_WORK_START
            ),
            TaskTemplateEntry(
                TaskTemplateDefaults.WINTER_END_HOUR,
                TaskTemplateDefaults.WINTER_END_MINUTE,
                TaskTemplateDefaults.LABEL_WORK_END
            )
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
