package com.ddtask.scheduler.util

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import com.ddtask.scheduler.R

object EmailConfigHelper {

    data class EmailFormData(
        val recipient: String,
        val sender: String,
        val password: String,
        val host: String,
        val port: Int
    )

    fun loadFromStorage(storage: NotificationStorage): EmailFormData {
        return EmailFormData(
            recipient = storage.recipientEmail,
            sender = storage.senderEmail,
            password = storage.senderPassword,
            host = storage.smtpHost,
            port = storage.smtpPort
        )
    }

    fun save(context: Context, storage: NotificationStorage, form: EmailFormData): Boolean {
        if (!isValidEmail(form.recipient)) {
            Toast.makeText(context, R.string.recipient_email_invalid, Toast.LENGTH_SHORT).show()
            return false
        }
        if (!isValidEmail(form.sender)) {
            Toast.makeText(context, R.string.sender_email_invalid, Toast.LENGTH_SHORT).show()
            return false
        }
        if (form.password.isBlank()) {
            Toast.makeText(context, R.string.sender_password_required, Toast.LENGTH_SHORT).show()
            return false
        }
        if (form.host.isBlank()) {
            Toast.makeText(context, R.string.smtp_host_required, Toast.LENGTH_SHORT).show()
            return false
        }
        if (form.port !in 1..65535) {
            Toast.makeText(context, R.string.smtp_port_invalid, Toast.LENGTH_SHORT).show()
            return false
        }

        storage.recipientEmail = form.recipient
        storage.senderEmail = form.sender
        storage.senderPassword = form.password
        storage.smtpHost = form.host
        storage.smtpPort = form.port
        Toast.makeText(context, R.string.email_settings_saved, Toast.LENGTH_SHORT).show()
        return true
    }

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
