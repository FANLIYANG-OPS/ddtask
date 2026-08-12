package com.ddtask.scheduler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ddtask.scheduler.service.AlarmScheduler

/** 开机完成后重新注册所有已启用任务的闹钟（系统重启会清除 AlarmManager）。 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        AlarmScheduler(context).rescheduleAll()
    }
}
