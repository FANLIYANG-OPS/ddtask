package com.ddtask.scheduler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.LaunchProxyActivity
import com.ddtask.scheduler.service.AlarmScheduler
import com.ddtask.scheduler.service.GoHomeScheduler
import com.ddtask.scheduler.service.ScreenControlService
import com.ddtask.scheduler.util.SettingsStorage
import com.ddtask.scheduler.util.TaskStorage

/** 定时任务到点广播：可选常亮 → 打开钉钉 → 延迟隐藏 → 按重复模式重新调度或禁用。 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        if (intent.action != null && intent.action != ACTION_ALARM) return

        val taskStorage = TaskStorage(context)
        val task = taskStorage.getById(taskId) ?: return
        if (!task.enabled) return

        val settings = SettingsStorage(context)
        if (settings.keepScreenOn) {
            val serviceIntent = Intent(context, ScreenControlService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        val launchIntent = Intent(context, LaunchProxyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launchIntent)

        GoHomeScheduler(context).scheduleHideSequence(taskId)

        if (task.shouldRescheduleAfterRun()) {
            AlarmScheduler(context).schedule(task)
        } else {
            taskStorage.update(task.copy(enabled = false))
        }
    }

    companion object {
        const val ACTION_ALARM = "com.ddtask.scheduler.ACTION_ALARM"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
