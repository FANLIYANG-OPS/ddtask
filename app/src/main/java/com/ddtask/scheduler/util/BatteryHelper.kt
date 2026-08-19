package com.ddtask.scheduler.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** 读取当前手机电量，供邮件正文使用。 */
object BatteryHelper {

    private const val PERCENT_MULTIPLIER = 100
    private const val UNKNOWN_LEVEL = -1

    fun levelText(context: Context): String {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return "未知"
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, UNKNOWN_LEVEL)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, UNKNOWN_LEVEL)
        if (level < 0 || scale <= 0) return "未知"
        val percent = level * PERCENT_MULTIPLIER / scale
        return "${percent}%"
    }
}
