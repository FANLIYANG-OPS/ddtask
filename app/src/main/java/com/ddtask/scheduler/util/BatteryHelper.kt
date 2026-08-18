package com.ddtask.scheduler.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** 读取当前手机电量，供邮件正文使用。 */
object BatteryHelper {

    fun levelText(context: Context): String {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return "未知"
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return "未知"
        val percent = level * 100 / scale
        return "${percent}%"
    }
}
