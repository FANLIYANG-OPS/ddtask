package com.ddtask.scheduler.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.ddtask.scheduler.R
import com.ddtask.scheduler.util.ClockInActionHandler
import com.ddtask.scheduler.util.ClockInSource
import com.ddtask.scheduler.util.EmailReceiver
import com.ddtask.scheduler.util.NotificationIds
import com.ddtask.scheduler.util.NotificationStorage
import com.ddtask.scheduler.util.TimeConstants

/**
 * 前台服务：定期 IMAP 轮询发件邮箱收件箱。
 * 不依赖 QQ/微信等第三方 App 是否在后台存活。
 */
class EmailPollingService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var polling = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!polling) return
            Thread {
                pollOnce()
                mainHandler.postDelayed(this, POLL_INTERVAL_MS)
            }.start()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPolling()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NotificationIds.EMAIL_POLLING, createNotification())
        }
        startPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        stopPolling()
        super.onDestroy()
    }

    private fun startPolling() {
        if (polling) return
        polling = true
        mainHandler.post(pollRunnable)
    }

    private fun stopPolling() {
        polling = false
        mainHandler.removeCallbacks(pollRunnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
    }

    private fun pollOnce() {
        val storage = NotificationStorage(this)
        if (!storage.emailTriggerEnabled || !storage.isSenderMailboxReady()) return

        val result = EmailReceiver.pollNewMessages(this)
        storage.recordImapPoll(result.error)

        if (result.emails.isEmpty()) return

        val handler = ClockInActionHandler(this)
        result.emails.forEach { email ->
            handler.handleIncomingText(email.body, ClockInSource.EMAIL)
            storage.recordEmailTrigger(email.body)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.email_polling_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(this)
        }
        return builder
            .setContentTitle(getString(R.string.email_polling_title))
            .setContentText(getString(R.string.email_polling_desc))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.ddtask.scheduler.STOP_EMAIL_POLLING"
        private const val CHANNEL_ID = "email_polling"
        private const val POLL_INTERVAL_MS = 30 * TimeConstants.ONE_SECOND_MS
    }
}
