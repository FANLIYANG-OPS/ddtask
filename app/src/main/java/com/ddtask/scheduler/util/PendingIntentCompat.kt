package com.ddtask.scheduler.util

import android.app.PendingIntent
import android.os.Build

/** PendingIntent 标志兼容：FLAG_IMMUTABLE 仅 Android 12+ 需要。 */
object PendingIntentCompat {

    fun updateCurrentImmutable(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
    }

    private fun immutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }
}
