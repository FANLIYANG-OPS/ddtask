package com.ddtask.scheduler.fragment

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.FragmentNotificationsBinding
import com.ddtask.scheduler.util.EmailSender
import com.ddtask.scheduler.util.NotificationAccess
import com.ddtask.scheduler.util.NotificationStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationStorage: NotificationStorage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationStorage = NotificationStorage(requireContext())
        loadSettings()
        binding.btnGrantListener.setOnClickListener {
            NotificationAccess.openSettings(requireContext())
        }
        binding.btnSaveEmail.setOnClickListener { saveSettings() }
        binding.btnTestEmail.setOnClickListener { sendTestEmail() }
        binding.switchEmailNotify.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureReadyForEnable()) {
                binding.switchEmailNotify.isChecked = false
                return@setOnCheckedChangeListener
            }
            notificationStorage.emailNotifyEnabled = isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        updateListenerStatus()
        updateLastSentStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadSettings() {
        binding.switchEmailNotify.setOnCheckedChangeListener(null)
        binding.switchEmailNotify.isChecked = notificationStorage.emailNotifyEnabled
        binding.switchEmailNotify.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureReadyForEnable()) {
                binding.switchEmailNotify.isChecked = false
                return@setOnCheckedChangeListener
            }
            notificationStorage.emailNotifyEnabled = isChecked
        }

        binding.etRecipientEmail.setText(notificationStorage.recipientEmail)
        binding.etSenderEmail.setText(notificationStorage.senderEmail)
        binding.etSenderPassword.setText(notificationStorage.senderPassword)
        binding.etSmtpHost.setText(notificationStorage.smtpHost)
        binding.etSmtpPort.setText(notificationStorage.smtpPort.toString())
    }

    private fun saveSettings(): Boolean {
        val recipient = binding.etRecipientEmail.text?.toString()?.trim().orEmpty()
        val sender = binding.etSenderEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etSenderPassword.text?.toString().orEmpty()
        val host = binding.etSmtpHost.text?.toString()?.trim().orEmpty()
        val portText = binding.etSmtpPort.text?.toString()?.trim().orEmpty()

        if (!isValidEmail(recipient)) {
            Toast.makeText(requireContext(), R.string.recipient_email_invalid, Toast.LENGTH_SHORT).show()
            return false
        }
        if (!isValidEmail(sender)) {
            Toast.makeText(requireContext(), R.string.sender_email_invalid, Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isBlank()) {
            Toast.makeText(requireContext(), R.string.sender_password_required, Toast.LENGTH_SHORT).show()
            return false
        }
        if (host.isBlank()) {
            Toast.makeText(requireContext(), R.string.smtp_host_required, Toast.LENGTH_SHORT).show()
            return false
        }

        val port = portText.toIntOrNull()
        if (port == null || port !in 1..65535) {
            Toast.makeText(requireContext(), R.string.smtp_port_invalid, Toast.LENGTH_SHORT).show()
            return false
        }

        notificationStorage.recipientEmail = recipient
        notificationStorage.senderEmail = sender
        notificationStorage.senderPassword = password
        notificationStorage.smtpHost = host
        notificationStorage.smtpPort = port

        Toast.makeText(requireContext(), R.string.email_settings_saved, Toast.LENGTH_SHORT).show()
        return true
    }

    private fun sendTestEmail() {
        if (!saveSettings()) return
        if (!NotificationAccess.isEnabled(requireContext())) {
            showListenerRequiredDialog()
            return
        }

        binding.btnTestEmail.isEnabled = false
        EmailSender.sendTestEmail(requireContext()) { success, error ->
            activity?.runOnUiThread {
                binding.btnTestEmail.isEnabled = true
                if (success) {
                    Toast.makeText(requireContext(), R.string.email_test_sent, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.email_send_failed, error.orEmpty()),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun ensureReadyForEnable(): Boolean {
        if (!saveSettings()) return false
        if (!NotificationAccess.isEnabled(requireContext())) {
            showListenerRequiredDialog()
            return false
        }
        return true
    }

    private fun showListenerRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_access_required_title)
            .setMessage(R.string.notification_access_required_message)
            .setPositiveButton(R.string.grant_notification_access) { _, _ ->
                NotificationAccess.openSettings(requireContext())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateListenerStatus() {
        val enabled = NotificationAccess.isEnabled(requireContext())
        binding.statusListener.text = if (enabled) {
            getString(R.string.notification_listener_enabled)
        } else {
            getString(R.string.notification_listener_disabled)
        }
        binding.statusListener.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (enabled) R.color.status_ok else R.color.status_error
            )
        )
    }

    private fun updateLastSentStatus() {
        val lastSentAt = notificationStorage.lastSentAt
        if (lastSentAt <= 0L) {
            binding.tvLastSent.visibility = View.GONE
            return
        }
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastSentAt))
        binding.tvLastSent.visibility = View.VISIBLE
        binding.tvLastSent.text = getString(
            R.string.last_email_sent,
            time,
            notificationStorage.lastSentSummary
        )
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
