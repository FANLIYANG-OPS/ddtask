package com.ddtask.scheduler.util

import android.content.Context
import android.content.Intent

/** 检测钉钉是否安装并通过 Launcher Intent 启动。 */
object DingTalkLauncher {

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(ClockInDetector.DINGTALK_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launch(context: Context): Boolean {
        if (!isInstalled(context)) return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(ClockInDetector.DINGTALK_PACKAGE)
            ?: return false
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        context.startActivity(launchIntent)
        return true
    }
}
