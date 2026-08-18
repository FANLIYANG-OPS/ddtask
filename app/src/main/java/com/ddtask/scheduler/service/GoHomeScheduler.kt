package com.ddtask.scheduler.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.receiver.GoHomeReceiver
import com.ddtask.scheduler.util.PendingIntentCompat

/**
 * 定时打开钉钉后的延迟操作调度。
 * 打开钉钉约 1 分钟后短暂回到本应用，再重新拉起钉钉以保持进程活跃。
 */
class GoHomeScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 隐藏流程：1分钟后回桌面 → 3秒后重新打开钉钉 → 2秒后再次回桌面。
     * 重新打开钉钉可保持进程活跃，确保下次定时任务能正常执行。
     */
    fun scheduleHideSequence(taskId: Long) {
        scheduleAt(taskId, GoHomeReceiver.ACTION_GO_HOME, HIDE_DELAY_MS, requestCodeHide(taskId))
        scheduleAt(
            taskId,
            GoHomeReceiver.ACTION_RELAUNCH_DINGTALK,
            HIDE_DELAY_MS + RELAUNCH_DELAY_MS,
            requestCodeRelaunch(taskId)
        )
        scheduleAt(
            taskId,
            GoHomeReceiver.ACTION_GO_HOME,
            HIDE_DELAY_MS + RELAUNCH_DELAY_MS + HIDE_AGAIN_DELAY_MS,
            requestCodeHideAgain(taskId)
        )
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(createPendingIntent(taskId, GoHomeReceiver.ACTION_GO_HOME, requestCodeHide(taskId)))
        alarmManager.cancel(
            createPendingIntent(taskId, GoHomeReceiver.ACTION_RELAUNCH_DINGTALK, requestCodeRelaunch(taskId))
        )
        alarmManager.cancel(createPendingIntent(taskId, GoHomeReceiver.ACTION_GO_HOME, requestCodeHideAgain(taskId)))
    }

    private fun scheduleAt(taskId: Long, action: String, delayMs: Long, requestCode: Int) {
        val triggerAt = System.currentTimeMillis() + delayMs
        val pendingIntent = createPendingIntent(taskId, action, requestCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun createPendingIntent(taskId: Long, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, GoHomeReceiver::class.java).apply {
            this.action = action
            putExtra(GoHomeReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntentCompat.updateCurrentImmutable()
        )
    }

    companion object {
        private const val HIDE_DELAY_MS = 60_000L
        private const val RELAUNCH_DELAY_MS = 3_000L
        private const val HIDE_AGAIN_DELAY_MS = 2_000L

        private const val REQUEST_HIDE_BASE = 500_000
        private const val REQUEST_RELAUNCH_BASE = 510_000
        private const val REQUEST_HIDE_AGAIN_BASE = 520_000

        fun requestCodeHide(taskId: Long): Int = REQUEST_HIDE_BASE + taskId.toInt()
        fun requestCodeRelaunch(taskId: Long): Int = REQUEST_RELAUNCH_BASE + taskId.toInt()
        fun requestCodeHideAgain(taskId: Long): Int = REQUEST_HIDE_AGAIN_BASE + taskId.toInt()
    }
}
