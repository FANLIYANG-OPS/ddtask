package com.ddtask.scheduler.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.FragmentNotificationsBinding
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
        updateEmailConfigStatus()
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
    }

    private fun ensureReadyForEnable(): Boolean {
        if (!notificationStorage.isConfigured()) {
            showEmailConfigRequiredDialog()
            return false
        }
        if (!NotificationAccess.isEnabled(requireContext())) {
            showListenerRequiredDialog()
            return false
        }
        return true
    }

    private fun showEmailConfigRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.email_config_required_title)
            .setMessage(R.string.email_config_required_message)
            .setPositiveButton(R.string.go_settings) { _, _ ->
                (activity as? MainActivity)?.openTab(MainActivity.TAB_SETTINGS)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

    private fun updateEmailConfigStatus() {
        val configured = notificationStorage.isConfigured()
        binding.statusEmailConfig.text = if (configured) {
            getString(
                R.string.email_config_status_ok,
                notificationStorage.senderEmail,
                notificationStorage.recipientEmail
            )
        } else {
            getString(R.string.email_config_status_missing)
        }
        binding.statusEmailConfig.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (configured) R.color.status_ok else R.color.status_error
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
}
