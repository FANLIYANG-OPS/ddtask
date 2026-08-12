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

object EmailSender {

    fun sendClockInSuccess(
        context: Context,
        notificationText: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val storage = NotificationStorage(context)
        val subject = context.getString(R.string.email_clock_in_subject)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val body = context.getString(
            R.string.email_clock_in_body,
            time,
            notificationText.trim()
        )
        sendAsync(storage, subject, body, onComplete)
    }

    fun sendTestEmail(
        context: Context,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val storage = NotificationStorage(context)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val subject = context.getString(R.string.email_test_subject)
        val body = context.getString(R.string.email_test_body, time)
        sendAsync(storage, subject, body, onComplete)
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
            put("mail.smtp.host", storage.smtpHost)
            put("mail.smtp.port", storage.smtpPort.toString())
            put("mail.smtp.auth", "true")
            if (storage.smtpPort == 465) {
                put("mail.smtp.socketFactory.port", "465")
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.socketFactory.fallback", "false")
            } else {
                put("mail.smtp.starttls.enable", "true")
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
            setSubject(subject, "UTF-8")
            setText(body, "UTF-8")
        }

        Transport.send(message)
    }
}
