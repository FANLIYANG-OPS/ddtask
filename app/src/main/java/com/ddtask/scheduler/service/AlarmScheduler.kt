package com.ddtask.scheduler.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.model.ScheduledTask
import com.ddtask.scheduler.receiver.AlarmReceiver
import com.ddtask.scheduler.util.ScheduleCalculator
import com.ddtask.scheduler.util.TaskStorage

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val taskStorage = TaskStorage(context)
    private val scheduleCalculator = ScheduleCalculator(context)

    fun schedule(task: ScheduledTask) {
        if (!task.enabled) {
            cancel(task.id)
            return
        }

        val triggerAt = scheduleCalculator.nextTriggerTime(task) ?: return
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
}
