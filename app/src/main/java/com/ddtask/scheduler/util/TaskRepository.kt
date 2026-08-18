package com.ddtask.scheduler.util

import android.content.Context
import com.ddtask.scheduler.model.RepeatMode
import com.ddtask.scheduler.model.ScheduledTask
import com.ddtask.scheduler.service.AlarmScheduler

/** 新建任务参数；每个实例对应一条完全独立的定时任务。 */
data class NewTaskSpec(
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatMode: RepeatMode = RepeatMode.DAILY,
    val cronExpression: String = ""
) {
    fun toScheduledTask(id: Long): ScheduledTask {
        return ScheduledTask(
            id = id,
            hour = hour,
            minute = minute,
            label = label,
            enabled = enabled,
            repeatDaily = repeatMode == RepeatMode.DAILY,
            repeatMode = repeatMode.key,
            cronExpression = cronExpression
        )
    }
}

data class SetEnabledResult(
    val task: ScheduledTask,
    /** 开启时闹钟注册失败（如无精确闹钟权限） */
    val scheduleFailed: Boolean = false
)

/**
 * 定时任务增删改查与闹钟同步的唯一入口。
 * 模板批量创建、自定义创建、编辑、开关均走此类，保证每条任务互不影响。
 */
class TaskRepository(context: Context) {

    private val storage = TaskStorage(context.applicationContext)
    private val alarmScheduler = AlarmScheduler(context.applicationContext)

    fun list(): List<ScheduledTask> {
        return storage.getAll().sortedWith(
            compareBy({ it.hour * 60 + it.minute }, { it.id })
        )
    }

    fun getById(id: Long): ScheduledTask? = storage.getById(id)

    /** 创建单条任务（与模板无关的自定义添加）。 */
    fun create(spec: NewTaskSpec): ScheduledTask {
        val task = spec.toScheduledTask(storage.nextId())
        storage.add(task)
        alarmScheduler.schedule(task)
        return task
    }

    /**
     * 一次创建多条互不影响的任务（如模板上班/下班）。
     * 在同一事务内分配不同 id 并写入，避免重复 id 或覆盖。
     */
    fun createIndependent(specs: List<NewTaskSpec>): List<ScheduledTask> {
        if (specs.isEmpty()) return emptyList()
        val ids = storage.nextIds(specs.size)
        val tasks = specs.mapIndexed { index, spec ->
            spec.toScheduledTask(ids[index])
        }
        storage.addAll(tasks)
        tasks.forEach { alarmScheduler.schedule(it) }
        return tasks
    }

    /** 更新任务并重新注册闹钟。 */
    fun update(task: ScheduledTask): ScheduledTask {
        alarmScheduler.cancel(task.id)
        storage.update(task)
        if (task.enabled) {
            alarmScheduler.schedule(task)
        }
        return task
    }

    /** 切换启用状态；仅影响 [task.id] 对应的那一条。 */
    fun setEnabled(task: ScheduledTask, enabled: Boolean): SetEnabledResult {
        if (!enabled) {
            alarmScheduler.cancel(task.id)
            val updated = task.copy(enabled = false)
            storage.update(updated)
            return SetEnabledResult(updated)
        }
        val updated = task.copy(enabled = true)
        storage.update(updated)
        val scheduled = alarmScheduler.schedule(updated)
        if (!scheduled) {
            val reverted = task.copy(enabled = false)
            storage.update(reverted)
            return SetEnabledResult(reverted, scheduleFailed = true)
        }
        return SetEnabledResult(updated)
    }

    /** 删除单条任务，不影响其它任务。 */
    fun delete(id: Long) {
        alarmScheduler.cancel(id)
        storage.delete(id)
    }
}
