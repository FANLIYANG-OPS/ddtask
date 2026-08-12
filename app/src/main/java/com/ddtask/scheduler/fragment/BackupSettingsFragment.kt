package com.ddtask.scheduler.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ddtask.scheduler.BuildConfig
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.DialogConfigIoBinding
import com.ddtask.scheduler.databinding.FragmentSettingsBackupBinding
import com.ddtask.scheduler.util.ConfigManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BackupSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBackupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvVersion.text = getString(
            R.string.version_label,
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        binding.btnExportConfig.setOnClickListener { showExportDialog() }
        binding.btnImportConfig.setOnClickListener { showImportConfirmDialog() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            (parentFragment as? SettingsHostFragment)?.childFragmentManager?.fragments?.forEach { fragment ->
                when (fragment) {
                    is EmailSettingsFragment -> fragment.resetEmailUi()
                    is NotificationSettingsFragment -> fragment.refreshAfterImport()
                }
            }
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
