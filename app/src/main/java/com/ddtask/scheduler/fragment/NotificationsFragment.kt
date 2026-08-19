package com.ddtask.scheduler.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.FragmentNotificationsBinding
import com.ddtask.scheduler.util.BrightnessController
import com.ddtask.scheduler.util.DateFormats
import com.ddtask.scheduler.util.DingTalkLauncher
import com.ddtask.scheduler.util.KeepScreenOverlay
import com.ddtask.scheduler.util.EmailPollingController
import com.ddtask.scheduler.util.NotificationAccess
import com.ddtask.scheduler.util.NotificationStorage
import com.ddtask.scheduler.util.SettingsStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 通知 Tab：手动打卡 / 打卡通知开关，以及钉钉状态与屏幕相关通用设置。 */
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationStorage: NotificationStorage
    private lateinit var settingsStorage: SettingsStorage

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
        settingsStorage = SettingsStorage(requireContext())
        setupScreenSettings()
        loadSwitches()
    }

    override fun onResume() {
        super.onResume()
        loadSwitches()
        updateDingTalkStatus()
        syncScreenSettingsUi()
        updateLastOpenStatus()
        updateLastSentStatus()
        updateEmailTriggerUi()
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

        binding.switchEmailTrigger.setOnCheckedChangeListener(null)
        binding.switchEmailTrigger.isChecked = notificationStorage.emailTriggerEnabled
        binding.switchEmailTrigger.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureEmailConfigured()) {
                binding.switchEmailTrigger.isChecked = false
                return@setOnCheckedChangeListener
            }
            notificationStorage.emailTriggerEnabled = isChecked
            if (isChecked) {
                notificationStorage.resetImapCursor()
            }
            EmailPollingController.sync(requireContext())
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

        binding.switchCloseDingtalk.setOnCheckedChangeListener(null)
        binding.switchCloseDingtalk.isChecked = notificationStorage.closeDingTalkEnabled
        binding.switchCloseDingtalk.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureCloseDingTalkReady()) {
                binding.switchCloseDingtalk.isChecked = false
                return@setOnCheckedChangeListener
            }
            notificationStorage.closeDingTalkEnabled = isChecked
        }
    }

    private fun setupScreenSettings() {
        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            handleKeepScreenOnToggle(isChecked)
        }
        binding.switchDimScreen.setOnCheckedChangeListener { _, isChecked ->
            handleDimScreenToggle(isChecked)
        }
    }

    private fun syncScreenSettingsUi() {
        binding.switchKeepScreenOn.setOnCheckedChangeListener(null)
        binding.switchKeepScreenOn.isChecked = settingsStorage.keepScreenOn
        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            handleKeepScreenOnToggle(isChecked)
        }

        binding.switchDimScreen.setOnCheckedChangeListener(null)
        binding.switchDimScreen.isChecked = settingsStorage.dimScreen
        binding.switchDimScreen.setOnCheckedChangeListener { _, isChecked ->
            handleDimScreenToggle(isChecked)
        }

        if (settingsStorage.dimScreen && BrightnessController.canWriteSettings(requireContext())) {
            BrightnessController.setMinimumBrightness(requireContext())
        }
    }

    /** 开启手动打卡前确认通知监听已授权。 */
    private fun ensureListenerEnabled(): Boolean {
        if (!NotificationAccess.isEnabled(requireContext())) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.notification_access_required_title)
                .setMessage(R.string.notification_access_required_message)
                .setPositiveButton(R.string.grant_notification_access) { _, _ ->
                    (activity as? MainActivity)?.openPermissionSetup()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return false
        }
        return true
    }

    /** 开启打卡通知前确认 SMTP 已配置且通知监听可用。 */
    private fun ensureEmailReady(): Boolean {
        if (!notificationStorage.isConfigured()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.email_config_required_title)
                .setMessage(R.string.email_config_required_message)
                .setPositiveButton(R.string.go_settings) { _, _ ->
                    (activity as? MainActivity)?.openTab(MainActivity.TAB_SETTINGS)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return false
        }
        return ensureListenerEnabled()
    }

    /** 开启关闭钉钉前确认邮件已配置（返回可通过通知或邮件触发）。 */
    private fun ensureCloseDingTalkReady(): Boolean {
        if (!notificationStorage.isConfigured()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.email_config_required_title)
                .setMessage(R.string.email_config_required_message)
                .setPositiveButton(R.string.go_settings) { _, _ ->
                    (activity as? MainActivity)?.openTab(MainActivity.TAB_SETTINGS)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return false
        }
        return true
    }

    private fun ensureEmailConfigured(): Boolean {
        if (notificationStorage.isSenderMailboxReady()) return true
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.email_config_required_title)
            .setMessage(R.string.email_trigger_config_required_message)
            .setPositiveButton(R.string.go_settings) { _, _ ->
                (activity as? MainActivity)?.openTab(MainActivity.TAB_SETTINGS)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
        return false
    }

    private fun updateDingTalkStatus() {
        val installed = DingTalkLauncher.isInstalled(requireContext())
        binding.statusDingtalk.text = if (installed) {
            getString(R.string.dingtalk_installed)
        } else {
            getString(R.string.dingtalk_not_installed)
        }
        binding.statusDingtalk.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (installed) R.color.status_ok else R.color.status_error
            )
        )
    }

    private fun handleKeepScreenOnToggle(enabled: Boolean) {
        if (!enabled) {
            settingsStorage.keepScreenOn = false
            return
        }
        val hasWriteSettings = BrightnessController.canWriteSettings(requireContext())
        val hasOverlay = KeepScreenOverlay(requireContext()).canDrawOverlay()
        if (hasWriteSettings && hasOverlay) {
            settingsStorage.keepScreenOn = true
            return
        }
        binding.switchKeepScreenOn.isChecked = false
        showKeepScreenOnPermissionDialog(!hasWriteSettings, !hasOverlay)
    }

    private fun showKeepScreenOnPermissionDialog(needWriteSettings: Boolean, needOverlay: Boolean) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.keep_screen_on_permission_title)
            .setMessage(R.string.keep_screen_on_permission_message)
            .setPositiveButton(R.string.go_settings) { _, _ ->
                if (needWriteSettings) {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    })
                } else if (needOverlay) {
                    requestOverlayPermission()
                }
            }
            .setNeutralButton(R.string.grant_overlay_permission) { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            })
        }
    }

    private fun handleDimScreenToggle(enabled: Boolean) {
        if (enabled) {
            if (!BrightnessController.canWriteSettings(requireContext())) {
                binding.switchDimScreen.isChecked = false
                showWriteSettingsDialog()
                return
            }
            BrightnessController.saveCurrentBrightness(requireContext(), settingsStorage)
            BrightnessController.setMinimumBrightness(requireContext())
            settingsStorage.dimScreen = true
        } else {
            settingsStorage.dimScreen = false
            BrightnessController.restoreBrightness(requireContext(), settingsStorage)
        }
    }

    private fun showWriteSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.write_settings_title)
            .setMessage(R.string.write_settings_message)
            .setPositiveButton(R.string.go_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                })
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
        val time = SimpleDateFormat(DateFormats.DATETIME, Locale.getDefault()).format(Date(lastOpenAt))
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
        val time = SimpleDateFormat(DateFormats.DATETIME, Locale.getDefault()).format(Date(lastSentAt))
        binding.tvLastSent.visibility = View.VISIBLE
        binding.tvLastSent.text = getString(
            R.string.last_email_sent,
            time,
            notificationStorage.lastSentSummary
        )
    }

    private fun updateEmailTriggerUi() {
        val sender = notificationStorage.senderEmail
        if (sender.isNotBlank() && notificationStorage.emailTriggerEnabled) {
            binding.tvEmailTriggerAddress.visibility = View.VISIBLE
            binding.tvEmailTriggerAddress.text = getString(R.string.email_trigger_address, sender)
            updateEmailPollStatus()
        } else {
            binding.tvEmailTriggerAddress.visibility = View.GONE
            binding.tvEmailPollStatus.visibility = View.GONE
        }

        val lastAt = notificationStorage.lastEmailTriggerAt
        if (lastAt <= 0L) {
            binding.tvLastEmailTrigger.visibility = View.GONE
            return
        }
        val time = SimpleDateFormat(DateFormats.DATETIME, Locale.getDefault()).format(Date(lastAt))
        binding.tvLastEmailTrigger.visibility = View.VISIBLE
        binding.tvLastEmailTrigger.text = getString(
            R.string.last_email_trigger,
            time,
            notificationStorage.lastEmailTriggerSummary
        )
    }

    private fun updateEmailPollStatus() {
        val pollAt = notificationStorage.lastImapPollAt
        if (pollAt <= 0L) {
            binding.tvEmailPollStatus.visibility = View.GONE
            return
        }
        val time = SimpleDateFormat(DateFormats.TIME_ONLY, Locale.getDefault()).format(Date(pollAt))
        val error = notificationStorage.lastImapPollError
        binding.tvEmailPollStatus.visibility = View.VISIBLE
        if (error.isNotBlank()) {
            binding.tvEmailPollStatus.text = getString(R.string.email_poll_status_error, error, time)
            binding.tvEmailPollStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.status_error)
            )
        } else {
            binding.tvEmailPollStatus.text = getString(R.string.email_poll_status_ok, time)
            binding.tvEmailPollStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.status_ok)
            )
        }
    }
}
