package com.ddtask.scheduler.util

/** PendingIntent / Broadcast 请求码基数，避免不同调度器之间冲突。 */
object PendingIntentRequestCodes {
    const val ALARM_BASE = 100_000
    const val ALARM_SHOW_BASE = 200_000
    const val GO_HOME_HIDE_BASE = 500_000
    const val GO_HOME_RELAUNCH_BASE = 510_000
    const val GO_HOME_HIDE_AGAIN_BASE = 520_000
    const val GO_HOME_SESSION_RETURN_BASE = 530_000
    const val SESSION_TIMEOUT = 600_000
    const val RETURN_TO_MAIN = 300_001
    const val SESSION_RETURN_CODE_MODULO = 10_000

    const val SCREEN_CONTROL_OPEN_APP = 0
    const val SCREEN_CONTROL_STOP = 1
}
