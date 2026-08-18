package com.ddtask.scheduler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ddtask.scheduler.util.ClockInSessionManager

/** 手动打卡会话 1 分钟超时：检查打卡/返回结果并发送失败邮件。 */
class ClockInSessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SESSION_TIMEOUT) return
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (sessionId < 0L) return
        ClockInSessionManager(context).onTimeout(sessionId)
    }

    companion object {
        const val ACTION_SESSION_TIMEOUT = "com.ddtask.scheduler.ACTION_CLOCK_IN_SESSION_TIMEOUT"
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
