package com.ddtask.scheduler.util

import android.app.Notification
import android.os.Bundle

object NotificationTextExtractor {

    fun extract(extras: Bundle): String {
        val parts = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach {
            parts.add(it.toString())
        }
        return parts.joinToString(" ")
    }
}
