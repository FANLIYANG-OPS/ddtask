package com.ddtask.scheduler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ddtask.scheduler.LaunchProxyActivity
import com.ddtask.scheduler.util.AppNavigator
import com.ddtask.scheduler.util.ClockInSessionManager
import com.ddtask.scheduler.util.DingTalkLauncher

/** 定时打开钉钉后的「回主界面 / 再次拉起钉钉」延迟广播接收器。 */
class GoHomeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_GO_HOME -> goToAppMain(context)
            ACTION_RELAUNCH_DINGTALK -> relaunchDingTalk(context)
            null -> {
                // 部分系统投递时 action 为空，忽略
            }
        }
    }

    /** 回到 DDTask 主界面，隐藏钉钉 */
    private fun goToAppMain(context: Context) {
        AppNavigator.goToMain(context)
        ClockInSessionManager(context).onAppForeground()
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
