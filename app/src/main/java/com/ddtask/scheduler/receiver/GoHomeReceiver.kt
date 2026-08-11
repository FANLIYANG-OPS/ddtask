package com.ddtask.scheduler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ddtask.scheduler.service.ScreenControlService

class GoHomeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GO_HOME) return

        context.startService(
            Intent(context, ScreenControlService::class.java).apply {
                action = ScreenControlService.ACTION_STOP
            }
        )

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(homeIntent)
    }

    companion object {
        const val ACTION_GO_HOME = "com.ddtask.scheduler.ACTION_GO_HOME"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
