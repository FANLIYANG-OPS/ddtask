package com.ddtask.scheduler.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        loadSwitches()
        binding.switchAutoOpenDingtalk.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureListenerEnabled()) {
                binding.switchAutoOpenDingtalk.isChecked = false
                return@setOnCheckedChangeListener
            }
            notificationStorage.autoOpenDingTalkEnabled = isChecked
        }
        binding.switchEmailNotify.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureEmailReady()) {
                binding.switchEmailNotify.isChecked = false
                return@setOnCheckedChangeListener
            }
            notificationStorage.emailNotifyEnabled = isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        loadSwitches()
        updateLastOpenStatus()
        updateLastSentStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadSwitches() {
        binding.switchAutoOpenDingtalk.setOnCheckedChangeListener(null)
        binding.switchAutoOpenDingtalk.isChecked = notificationStorage.autoOpenDingTalkEnabled
        binding.switchAutoOpenDingtalk.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureListenerEnabled()) {
                binding.switchAutoOpenDingtalk.isChecked = false
                return@setOnCheckedChangeListener
            }
            notificationStorage.autoOpenDingTalkEnabled = isChecked
        }

        binding.switchEmailNotify.setOnCheckedChangeListener(null)
        binding.switchEmailNotify.isChecked = notificationStorage.emailNotifyEnabled
        binding.switchEmailNotify.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureEmailReady()) {
                binding.switchEmailNotify.isChecked = false
                return@setOnCheckedChangeListener
            }
            notificationStorage.emailNotifyEnabled = isChecked
        }
    }

    private fun ensureListenerEnabled(): Boolean {
        if (!NotificationAccess.isEnabled(requireContext())) {
            showGoSettingsDialog(
                R.string.notification_access_required_title,
                R.string.notification_access_required_message,
                SettingsHostFragment.SECTION_NOTIFICATION
            )
            return false
        }
        return true
    }

    private fun ensureEmailReady(): Boolean {
        if (!notificationStorage.isConfigured()) {
            showGoSettingsDialog(
                R.string.email_config_required_title,
                R.string.email_config_required_message,
                SettingsHostFragment.SECTION_EMAIL
            )
            return false
        }
        return ensureListenerEnabled()
    }

    private fun showGoSettingsDialog(titleRes: Int, messageRes: Int, section: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setMessage(messageRes)
            .setPositiveButton(R.string.go_settings) { _, _ ->
                (activity as? MainActivity)?.openSettingsSection(section)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateLastOpenStatus() {
        val lastOpenAt = notificationStorage.lastOpenDingTalkAt
        if (lastOpenAt <= 0L) {
            binding.tvLastOpen.visibility = View.GONE
            return
        }
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastOpenAt))
        binding.tvLastOpen.visibility = View.VISIBLE
        binding.tvLastOpen.text = getString(
            R.string.last_open_dingtalk,
            time,
            notificationStorage.lastOpenDingTalkSummary
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
