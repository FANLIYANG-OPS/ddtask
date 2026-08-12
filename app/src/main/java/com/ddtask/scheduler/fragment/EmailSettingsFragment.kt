package com.ddtask.scheduler.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.FragmentSettingsEmailBinding
import com.ddtask.scheduler.util.EmailConfigHelper
import com.ddtask.scheduler.util.EmailSender
import com.ddtask.scheduler.util.NotificationStorage

class EmailSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsEmailBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationStorage: NotificationStorage
    private var emailEditing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificationStorage = NotificationStorage(requireContext())
        binding.btnSaveEmail.setOnClickListener { saveEmailSettings() }
        binding.btnEditEmail.setOnClickListener {
            emailEditing = true
            loadEmailForm()
            updateEmailUi()
        }
        binding.btnTestEmail.setOnClickListener { sendTestEmail() }
    }

    override fun onResume() {
        super.onResume()
        if (!emailEditing) {
            loadEmailForm()
        }
        updateEmailUi()
    }

    fun resetEmailUi() {
        emailEditing = false
        loadEmailForm()
        updateEmailUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
}
