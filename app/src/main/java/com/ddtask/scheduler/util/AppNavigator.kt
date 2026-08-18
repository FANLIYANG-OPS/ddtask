package com.ddtask.scheduler.util

import android.content.Context
import android.content.Intent
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.service.ScreenControlService

/** 将 DDTask 主界面带到前台。 */
object AppNavigator {

    fun goToMain(context: Context) {
        context.startService(
            Intent(context, ScreenControlService::class.java).apply {
                action = ScreenControlService.ACTION_STOP
            }
        )
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
        )
    }
}
