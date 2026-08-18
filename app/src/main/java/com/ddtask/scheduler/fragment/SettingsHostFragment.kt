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
import com.ddtask.scheduler.databinding.FragmentSettingsHostBinding
import com.ddtask.scheduler.util.ClockInDetector
import com.ddtask.scheduler.util.ConfigManager
import com.ddtask.scheduler.util.EmailConfigHelper
import com.ddtask.scheduler.util.EmailSender
import com.ddtask.scheduler.util.NotificationStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** 设置 Tab 单页：自上而下为监听配置、邮件配置、备份导入导出。 */
class SettingsHostFragment : Fragment() {

    private var _binding: FragmentSettingsHostBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationStorage: NotificationStorage
    /** 已保存后折叠表单，点击「编辑」再展开。 */
    private var keywordsEditing = false
    private var emailEditing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationStorage = NotificationStorage(requireContext())
        setupKeywordsSection()
        setupEmailSection()
        setupBackupSection()
    }

    override fun onResume() {
        super.onResume()
        if (!keywordsEditing) {
            loadKeywordForm()
        }
        updateKeywordsUi()
        if (!emailEditing) {
            loadEmailForm()
        }
        updateEmailUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupKeywordsSection() {
        binding.btnSaveKeywords.setOnClickListener { saveKeywords() }
        binding.btnEditKeywords.setOnClickListener {
            keywordsEditing = true
            loadKeywordForm()
            updateKeywordsUi()
        }
    }

    private fun loadKeywordForm() {
        binding.etTriggerKeywords.setText(notificationStorage.triggerKeywords)
        binding.etSuccessKeywords.setText(notificationStorage.successKeywords)
        binding.etReturnKeywords.setText(notificationStorage.returnKeywords)
    }

    private fun saveKeywords() {
        notificationStorage.triggerKeywords = binding.etTriggerKeywords.text?.toString()?.trim().orEmpty()
        notificationStorage.successKeywords = binding.etSuccessKeywords.text?.toString()?.trim().orEmpty()
        notificationStorage.returnKeywords = binding.etReturnKeywords.text?.toString()?.trim().orEmpty()
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
        val returnKw = ClockInDetector.parseKeywords(
            notificationStorage.returnKeywords,
            ClockInDetector.parseKeywords(
                notificationStorage.successKeywords,
                ClockInDetector.DEFAULT_SUCCESS_KEYWORDS
            )
        ).joinToString("、")
        return getString(R.string.keywords_config_summary, trigger, success, returnKw)
    }

    private fun setupEmailSection() {
        binding.btnSaveEmail.setOnClickListener { saveEmailSettings() }
        binding.btnEditEmail.setOnClickListener {
            emailEditing = true
            loadEmailForm()
            updateEmailUi()
        }
        binding.btnTestEmail.setOnClickListener { sendTestEmail() }
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

    private fun setupBackupSection() {
        binding.tvVersion.text = getString(
            R.string.version_label,
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        binding.btnExportConfig.setOnClickListener { showExportDialog() }
        binding.btnImportConfig.setOnClickListener { showImportConfirmDialog() }
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

    /** 导入成功后刷新监听与邮件两块的折叠状态。 */
    private fun importConfig(json: String) {
        try {
            ConfigManager(requireContext()).importJson(json)
            keywordsEditing = false
            emailEditing = false
            loadKeywordForm()
            loadEmailForm()
            updateKeywordsUi()
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
