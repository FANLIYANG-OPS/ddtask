package com.ddtask.scheduler.util

import android.content.Context
import android.content.Intent

object DingTalkLauncher {

    const val PACKAGE_NAME = "com.alibaba.android.rimet"

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PACKAGE_NAME, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launch(context: Context): Boolean {
        if (!isInstalled(context)) return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
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
