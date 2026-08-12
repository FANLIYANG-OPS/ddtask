package com.ddtask.scheduler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ddtask.scheduler.LaunchProxyActivity
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.service.ScreenControlService
import com.ddtask.scheduler.util.DingTalkLauncher

class GoHomeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_GO_HOME -> goToAppMain(context)
            ACTION_RELAUNCH_DINGTALK -> relaunchDingTalk(context)
        }
    }

    /** 回到 DDTask 主界面，隐藏钉钉 */
    private fun goToAppMain(context: Context) {
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

    private fun relaunchDingTalk(context: Context) {
        if (DingTalkLauncher.launch(context)) return
        context.startActivity(
            Intent(context, LaunchProxyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    companion object {
        const val ACTION_GO_HOME = "com.ddtask.scheduler.ACTION_GO_HOME"
        const val ACTION_RELAUNCH_DINGTALK = "com.ddtask.scheduler.ACTION_RELAUNCH_DINGTALK"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
