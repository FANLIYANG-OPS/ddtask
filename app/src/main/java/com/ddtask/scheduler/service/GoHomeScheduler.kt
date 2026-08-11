package com.ddtask.scheduler.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.receiver.GoHomeReceiver

class GoHomeScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(taskId: Long) {
        val triggerAt = System.currentTimeMillis() + HIDE_DELAY_MS
        val pendingIntent = createPendingIntent(taskId)

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
    }

    private fun createPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, GoHomeReceiver::class.java).apply {
            action = GoHomeReceiver.ACTION_GO_HOME
            putExtra(GoHomeReceiver.EXTRA_TASK_ID, taskId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            flags
        )
    }

    companion object {
        private const val HIDE_DELAY_MS = 60_000L
        private const val REQUEST_CODE_BASE = 500_000

        fun requestCode(taskId: Long): Int = REQUEST_CODE_BASE + taskId.toInt()
    }
}
