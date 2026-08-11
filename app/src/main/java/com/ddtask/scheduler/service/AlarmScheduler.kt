package com.ddtask.scheduler.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.model.ScheduledTask
import com.ddtask.scheduler.receiver.AlarmReceiver
import com.ddtask.scheduler.util.TaskStorage
import java.util.Calendar
import kotlin.random.Random

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val taskStorage = TaskStorage(context)

    fun schedule(task: ScheduledTask) {
        if (!task.enabled) {
            cancel(task.id)
            return
        }

        val triggerAt = nextTriggerTime(task.hour, task.minute)
        val pendingIntent = createPendingIntent(task.id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(createPendingIntent(taskId))
        GoHomeScheduler(context).cancel(taskId)
    }

    fun rescheduleAll() {
        taskStorage.getAll().forEach { schedule(it) }
    }

    private fun createPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, taskId.toInt(), intent, flags)
    }

    companion object {
        /** 随机偏移范围：±60 秒，避免固定时间像机器唤醒 */
        private const val JITTER_SECONDS = 60

        fun nextTriggerTime(hour: Int, minute: Int): Long {
            val base = baseTriggerTime(hour, minute)
            val jitterMs = Random.nextInt(-JITTER_SECONDS, JITTER_SECONDS + 1) * 1000L
            val withJitter = base + jitterMs
            return if (withJitter > System.currentTimeMillis()) {
                withJitter
            } else {
                // 负偏移导致已过期时，改用 0~60 秒的正向随机延迟
                base + Random.nextInt(0, JITTER_SECONDS + 1) * 1000L
            }
        }

        private fun baseTriggerTime(hour: Int, minute: Int): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }
    }
}
