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
import com.ddtask.scheduler.BuildConfig
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.FragmentSettingsBinding
import com.ddtask.scheduler.util.BrightnessController
import com.ddtask.scheduler.util.DingTalkLauncher
import com.ddtask.scheduler.util.KeepScreenOverlay
import com.ddtask.scheduler.util.SettingsStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsStorage: SettingsStorage

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
        binding.tvVersion.text = getString(
            R.string.version_label,
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        setupSettings()
    }

    override fun onResume() {
        super.onResume()
        updateDingTalkStatus()
        syncSettingsUi()
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
}
