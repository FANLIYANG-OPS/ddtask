package com.ddtask.scheduler.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.model.ScheduledTask
import com.ddtask.scheduler.receiver.AlarmReceiver
import com.ddtask.scheduler.util.ExactAlarmHelper
import com.ddtask.scheduler.util.PendingIntentCompat
import com.ddtask.scheduler.util.ScheduleCalculator
import com.ddtask.scheduler.util.TaskStorage

/** 使用 [AlarmManager] 注册/取消精确闹钟，每个任务 id 对应独立 PendingIntent。 */
class AlarmScheduler(private val context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val taskStorage = TaskStorage(appContext)
    private val scheduleCalculator = ScheduleCalculator(appContext)

    fun schedule(task: ScheduledTask): Boolean {
        if (!task.enabled) {
            cancel(task.id)
            return true
        }

        val triggerAt = scheduleCalculator.nextTriggerTime(task) ?: return false
        val pendingIntent = createPendingIntent(task.id)
        return try {
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
            true
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val showIntent = PendingIntent.getActivity(
                        appContext,
                        REQUEST_SHOW + task.id.toInt(),
                        Intent(appContext, MainActivity::class.java),
                        PendingIntentCompat.updateCurrentImmutable()
                    )
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                        pendingIntent
                    )
                    true
                } catch (_: SecurityException) {
                    false
                }
            } else {
                false
            }
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(createPendingIntent(taskId))
        GoHomeScheduler(appContext).cancel(taskId)
    }

    fun rescheduleAll() {
        // Android 6.0 无精确闹钟权限弹窗，直接注册；Android 12+ 需已授权
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !ExactAlarmHelper.canScheduleExactAlarms(appContext)
        ) {
            return
        }
        taskStorage.getAll().forEach { schedule(it) }
    }

    private fun createPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_ALARM_BASE + taskId.toInt(),
            intent,
            PendingIntentCompat.updateCurrentImmutable()
        )
    }

    companion object {
        private const val REQUEST_ALARM_BASE = 100_000
        private const val REQUEST_SHOW = 200_000
    }
}
