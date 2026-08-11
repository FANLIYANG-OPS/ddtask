package com.ddtask.scheduler.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.R
import com.ddtask.scheduler.util.ScreenKeepOnController

class ScreenControlService : Service() {

    private var keepOnController: ScreenKeepOnController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        keepOnController = ScreenKeepOnController(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                keepOnController?.stop()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        keepOnController?.start()

        return START_STICKY
    }

    override fun onDestroy() {
        keepOnController?.stop()
        keepOnController = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.screen_control_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.screen_control_channel_desc)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            pendingIntentFlags()
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ScreenControlService::class.java).apply { action = ACTION_STOP },
            pendingIntentFlags()
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.screen_keep_on_active))
            .setContentText(getString(R.string.screen_keep_on_desc))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.stop_screen_keep_on), stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }

    companion object {
        const val ACTION_STOP = "com.ddtask.scheduler.STOP_SCREEN_CONTROL"
        private const val CHANNEL_ID = "screen_control"
        private const val NOTIFICATION_ID = 1001
    }
}
