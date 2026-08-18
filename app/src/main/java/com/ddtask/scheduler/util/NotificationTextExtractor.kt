package com.ddtask.scheduler.util

import android.app.Notification
import android.os.Bundle

/** 合并 Notification extras 中的标题、正文、大文本等字段，供关键字匹配使用。 */
object NotificationTextExtractor {

    fun extract(extras: Bundle): String {
        val parts = linkedSetOf<String>()
        extras.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach {
            parts.add(it.toString())
        }
        // 兼容 Android 6.0 及部分第三方 App（QQ、短信等）的自定义字段
        for (key in extras.keySet()) {
            when (val value = extras.get(key)) {
                is CharSequence -> {
                    val text = value.toString().trim()
                    if (text.isNotEmpty()) parts.add(text)
                }
                is Array<*> -> value.filterIsInstance<CharSequence>().forEach {
                    val text = it.toString().trim()
                    if (text.isNotEmpty()) parts.add(text)
                }
            }
        }
        return parts.joinToString(" ")
    }
}
