package com.ddtask.scheduler.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ddtask.scheduler.R
import com.ddtask.scheduler.ReturnProxyActivity

/**
 * 短暂前台服务，用于从后台（如通知监听）合法拉起主界面。
 * Android 8.0 以下以前台服务非必须，直接 startService 后 startActivity 即可。
 */
class ReturnToAppService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
            startForeground(NOTIFICATION_ID, createNotification())
        }

        startActivity(
            Intent(this, ReturnProxyActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.return_to_app_channel),
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
            .setContentTitle(getString(R.string.return_to_app_title))
            .setContentText(getString(R.string.return_to_app_desc))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_RETURN = "com.ddtask.scheduler.ACTION_RETURN_TO_APP"
        private const val CHANNEL_ID = "return_to_app"
        private const val NOTIFICATION_ID = 2002
    }
}
