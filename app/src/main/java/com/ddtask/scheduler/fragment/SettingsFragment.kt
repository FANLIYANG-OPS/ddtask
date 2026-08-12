package com.ddtask.scheduler.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ddtask.scheduler.BuildConfig
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.DialogConfigIoBinding
import com.ddtask.scheduler.databinding.FragmentSettingsBinding
import com.ddtask.scheduler.util.BrightnessController
import com.ddtask.scheduler.util.ConfigManager
import com.ddtask.scheduler.util.DingTalkLauncher
import com.ddtask.scheduler.util.EmailConfigHelper
import com.ddtask.scheduler.util.EmailSender
import com.ddtask.scheduler.util.KeepScreenOverlay
import com.ddtask.scheduler.util.NotificationStorage
import com.ddtask.scheduler.util.SettingsStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsStorage: SettingsStorage
    private lateinit var notificationStorage: NotificationStorage
    private var emailEditing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsStorage = SettingsStorage(requireContext())
        notificationStorage = NotificationStorage(requireContext())
        binding.tvVersion.text = getString(
            R.string.version_label,
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        setupSettings()
        binding.btnSaveEmail.setOnClickListener { saveEmailSettings() }
        binding.btnEditEmail.setOnClickListener {
            emailEditing = true
            loadEmailForm()
            updateEmailUi()
        }
        binding.btnTestEmail.setOnClickListener { sendTestEmail() }
        binding.btnRecheckPermissions.setOnClickListener {
            (activity as? MainActivity)?.openPermissionSetup()
        }
        binding.btnExportConfig.setOnClickListener { showExportDialog() }
        binding.btnImportConfig.setOnClickListener { showImportConfirmDialog() }
    }

    override fun onResume() {
        super.onResume()
        updateDingTalkStatus()
        syncSettingsUi()
        if (!emailEditing) {
            loadEmailForm()
        }
        updateEmailUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSettings() {
        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            handleKeepScreenOnToggle(isChecked)
        }
        binding.switchDimScreen.setOnCheckedChangeListener { _, isChecked ->
            handleDimScreenToggle(isChecked)
        }
    }

    private fun syncSettingsUi() {
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

    private fun loadEmailForm() {
        val data = EmailConfigHelper.loadFromStorage(notificationStorage)
        binding.etRecipientEmail.setText(data.recipient)
        binding.etSenderEmail.setText(data.sender)
        binding.etSenderPassword.setText(data.password)
        binding.etSmtpHost.setText(data.host)
        binding.etSmtpPort.setText(data.port.toString())
    }

    private fun saveEmailSettings() {
        val form = EmailConfigHelper.EmailFormData(
            recipient = binding.etRecipientEmail.text?.toString()?.trim().orEmpty(),
            sender = binding.etSenderEmail.text?.toString()?.trim().orEmpty(),
            password = binding.etSenderPassword.text?.toString().orEmpty(),
            host = binding.etSmtpHost.text?.toString()?.trim().orEmpty(),
            port = binding.etSmtpPort.text?.toString()?.trim()?.toIntOrNull() ?: -1
        )
        if (!EmailConfigHelper.save(requireContext(), notificationStorage, form)) return
        emailEditing = false
        updateEmailUi()
    }

    private fun updateEmailUi() {
        val configured = notificationStorage.isConfigured()
        val showForm = !configured || emailEditing
        binding.layoutEmailForm.visibility = if (showForm) View.VISIBLE else View.GONE
        binding.layoutEmailSummary.visibility = if (configured && !emailEditing) View.VISIBLE else View.GONE
        if (configured && !emailEditing) {
            binding.tvEmailSummary.text = getString(
                R.string.email_config_summary,
                notificationStorage.senderEmail,
                notificationStorage.recipientEmail,
                notificationStorage.smtpHost,
                notificationStorage.smtpPort
            )
        }
    }

    private fun sendTestEmail() {
        if (!notificationStorage.isConfigured()) {
            Toast.makeText(requireContext(), R.string.email_config_status_missing, Toast.LENGTH_SHORT).show()
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

    private fun showExportDialog() {
        val dialogBinding = DialogConfigIoBinding.inflate(layoutInflater)
        dialogBinding.etConfigJson.setText(ConfigManager(requireContext()).exportJson())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_config)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.copy_config) { _, _ ->
                copyToClipboard(dialogBinding.etConfigJson.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun showImportConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_config)
            .setMessage(R.string.import_config_warning)
            .setPositiveButton(R.string.continue_text) { _, _ -> showImportInputDialog() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showImportInputDialog() {
        val dialogBinding = DialogConfigIoBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_config)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.import_config) { _, _ ->
                importConfig(dialogBinding.etConfigJson.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun importConfig(json: String) {
        try {
            ConfigManager(requireContext()).importJson(json)
            emailEditing = false
            syncSettingsUi()
            loadEmailForm()
            updateEmailUi()
            Toast.makeText(requireContext(), R.string.config_import_success, Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            val message = when (e.message) {
                "empty_json" -> getString(R.string.config_import_empty)
                "invalid_json" -> getString(R.string.config_import_invalid)
                "unsupported_version" -> getString(R.string.config_import_unsupported)
                else -> getString(R.string.config_import_failed, e.message.orEmpty())
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun copyToClipboard(text: String) {
        if (text.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ddtask_config", text))
        Toast.makeText(requireContext(), R.string.config_copied, Toast.LENGTH_SHORT).show()
    }
}
