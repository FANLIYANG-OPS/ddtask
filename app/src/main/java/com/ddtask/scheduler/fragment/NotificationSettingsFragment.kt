package com.ddtask.scheduler.fragment

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
import com.ddtask.scheduler.MainActivity
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.FragmentSettingsNotificationBinding
import com.ddtask.scheduler.util.BrightnessController
import com.ddtask.scheduler.util.ClockInDetector
import com.ddtask.scheduler.util.DingTalkLauncher
import com.ddtask.scheduler.util.KeepScreenOverlay
import com.ddtask.scheduler.util.NotificationAccess
import com.ddtask.scheduler.util.NotificationStorage
import com.ddtask.scheduler.util.SettingsStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NotificationSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsNotificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsStorage: SettingsStorage
    private lateinit var notificationStorage: NotificationStorage
    private var keywordsEditing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsStorage = SettingsStorage(requireContext())
        notificationStorage = NotificationStorage(requireContext())
        setupScreenSettings()
        binding.btnGrantListener.setOnClickListener {
            NotificationAccess.openSettings(requireContext())
        }
        binding.btnSaveKeywords.setOnClickListener { saveKeywords() }
        binding.btnEditKeywords.setOnClickListener {
            keywordsEditing = true
            loadKeywordForm()
            updateKeywordsUi()
        }
        binding.btnRecheckPermissions.setOnClickListener {
            (activity as? MainActivity)?.openPermissionSetup()
        }
    }

    override fun onResume() {
        super.onResume()
        updateListenerStatus()
        updateDingTalkStatus()
        syncScreenSettingsUi()
        if (!keywordsEditing) {
            loadKeywordForm()
        }
        updateKeywordsUi()
    }

    fun refreshAfterImport() {
        keywordsEditing = false
        loadKeywordForm()
        updateKeywordsUi()
        syncScreenSettingsUi()
        updateListenerStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    private fun loadKeywordForm() {
        binding.etTriggerKeywords.setText(notificationStorage.triggerKeywords)
        binding.etSuccessKeywords.setText(notificationStorage.successKeywords)
    }

    private fun saveKeywords() {
        notificationStorage.triggerKeywords = binding.etTriggerKeywords.text?.toString()?.trim().orEmpty()
        notificationStorage.successKeywords = binding.etSuccessKeywords.text?.toString()?.trim().orEmpty()
        notificationStorage.keywordsConfigured = true
        keywordsEditing = false
        updateKeywordsUi()
        Toast.makeText(requireContext(), R.string.notification_settings_saved, Toast.LENGTH_SHORT).show()
    }

    private fun updateKeywordsUi() {
        val configured = notificationStorage.keywordsConfigured
        val showForm = !configured || keywordsEditing
        binding.layoutKeywordsForm.visibility = if (showForm) View.VISIBLE else View.GONE
        binding.layoutKeywordsSummary.visibility = if (configured && !keywordsEditing) View.VISIBLE else View.GONE
        if (configured && !keywordsEditing) {
            binding.tvKeywordsSummary.text = buildKeywordsSummary()
        }
    }

    private fun buildKeywordsSummary(): String {
        val trigger = ClockInDetector.parseKeywords(
            notificationStorage.triggerKeywords,
            ClockInDetector.DEFAULT_TRIGGER_KEYWORDS
        ).joinToString("、")
        val success = ClockInDetector.parseKeywords(
            notificationStorage.successKeywords,
            ClockInDetector.DEFAULT_SUCCESS_KEYWORDS
        ).joinToString("、")
        return getString(R.string.keywords_config_summary, trigger, success)
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
}
