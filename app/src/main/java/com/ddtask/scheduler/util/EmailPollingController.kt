package com.ddtask.scheduler.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.ddtask.scheduler.service.EmailPollingService

/** 根据开关启停 [EmailPollingService]。 */
object EmailPollingController {

    fun sync(context: Context) {
        val storage = NotificationStorage(context.applicationContext)
        if (storage.emailTriggerEnabled && storage.isConfigured()) {
            start(context)
        } else {
            stop(context)
        }
    }

    fun start(context: Context) {
        val intent = Intent(context.applicationContext, EmailPollingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.applicationContext.startForegroundService(intent)
        } else {
            context.applicationContext.startService(intent)
        }
    }

    fun stop(context: Context) {
        context.applicationContext.startService(
            Intent(context.applicationContext, EmailPollingService::class.java).apply {
                action = EmailPollingService.ACTION_STOP
            }
        )
    }
}
