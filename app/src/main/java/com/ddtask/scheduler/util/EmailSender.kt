package com.ddtask.scheduler.util

import android.content.Context
import com.ddtask.scheduler.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/** 通过 JavaMail 在后台线程发送 SMTP 邮件（打卡/返回通知 / 测试邮件）。 */
object EmailSender {

    fun sendClockInSuccess(
        context: Context,
        notificationText: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val storage = NotificationStorage(context)
        val subject = context.getString(R.string.email_clock_in_subject)
        val time = nowText()
        val body = context.getString(
            R.string.email_clock_in_body,
            time,
            notificationText.trim(),
            BatteryHelper.levelText(context)
        )
        sendAsync(storage, subject, body, onComplete)
    }

    fun sendClockInFailure(
        context: Context,
        triggerSummary: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val storage = NotificationStorage(context)
        val subject = context.getString(R.string.email_clock_in_failure_subject)
        val body = context.getString(
            R.string.email_clock_in_failure_body,
            nowText(),
            triggerSummary.trim(),
            BatteryHelper.levelText(context)
        )
        sendAsync(storage, subject, body, onComplete)
    }

    fun sendReturnSuccess(
        context: Context,
        detail: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val storage = NotificationStorage(context)
        val subject = context.getString(R.string.email_return_success_subject)
        val body = context.getString(
            R.string.email_return_success_body,
            nowText(),
            detail.trim(),
            BatteryHelper.levelText(context)
        )
        sendAsync(storage, subject, body, onComplete)
    }

    fun sendReturnFailure(
        context: Context,
        triggerSummary: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val storage = NotificationStorage(context)
        val subject = context.getString(R.string.email_return_failure_subject)
        val body = context.getString(
            R.string.email_return_failure_body,
            nowText(),
            triggerSummary.trim(),
            BatteryHelper.levelText(context)
        )
        sendAsync(storage, subject, body, onComplete)
    }

    fun sendTestEmail(
        context: Context,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val storage = NotificationStorage(context)
        val subject = context.getString(R.string.email_test_subject)
        val body = context.getString(
            R.string.email_test_body,
            nowText(),
            BatteryHelper.levelText(context)
        )
        sendAsync(storage, subject, body, onComplete)
    }

    private fun nowText(): String {
        return SimpleDateFormat(DateFormats.DATETIME, Locale.getDefault()).format(Date())
    }

    private fun sendAsync(
        storage: NotificationStorage,
        subject: String,
        body: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        Thread {
            try {
                send(storage, subject, body)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message ?: e.javaClass.simpleName)
            }
        }.start()
    }

    @Throws(Exception::class)
    private fun send(storage: NotificationStorage, subject: String, body: String) {
        if (!storage.isConfigured()) {
            throw IllegalStateException("Email settings incomplete")
        }

        val props = Properties().apply {
            put(MailConstants.PROP_SMTP_HOST, storage.smtpHost)
            put(MailConstants.PROP_SMTP_PORT, storage.smtpPort.toString())
            put(MailConstants.PROP_SMTP_AUTH, MailConstants.PROP_VALUE_TRUE)
            if (storage.smtpPort == EmailDefaults.DEFAULT_SMTP_PORT) {
                put(
                    MailConstants.PROP_SMTP_SOCKET_FACTORY_PORT,
                    EmailDefaults.DEFAULT_SMTP_PORT.toString()
                )
                put(MailConstants.PROP_SMTP_SOCKET_FACTORY_CLASS, MailConstants.SSL_SOCKET_FACTORY_CLASS)
                put(MailConstants.PROP_SMTP_SOCKET_FACTORY_FALLBACK, MailConstants.PROP_VALUE_FALSE)
            } else {
                put(MailConstants.PROP_SMTP_STARTTLS_ENABLE, MailConstants.PROP_VALUE_TRUE)
            }
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(storage.senderEmail, storage.senderPassword)
            }
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(storage.senderEmail))
            setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(storage.recipientEmail)
            )
            setSubject(subject, MailConstants.CHARSET_UTF8)
            setText(body, MailConstants.CHARSET_UTF8)
        }

        Transport.send(message)
    }
}
