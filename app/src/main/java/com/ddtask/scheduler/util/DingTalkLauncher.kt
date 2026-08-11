package com.ddtask.scheduler.util

object DingTalkLauncher {

    const val PACKAGE_NAME = "com.alibaba.android.rimet"

    fun isInstalled(context: android.content.Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PACKAGE_NAME, 0)
            true
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }
}
