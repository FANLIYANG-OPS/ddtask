package com.ddtask.scheduler.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.FragmentSettingsNotificationBinding
import com.ddtask.scheduler.util.ClockInDetector
import com.ddtask.scheduler.util.NotificationStorage

class NotificationSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsNotificationBinding? = null
    private val binding get() = _binding!!

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
        notificationStorage = NotificationStorage(requireContext())
        binding.btnSaveKeywords.setOnClickListener { saveKeywords() }
        binding.btnEditKeywords.setOnClickListener {
            keywordsEditing = true
            loadKeywordForm()
            updateKeywordsUi()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!keywordsEditing) {
            loadKeywordForm()
        }
        updateKeywordsUi()
    }

    fun refreshAfterImport() {
        keywordsEditing = false
        loadKeywordForm()
        updateKeywordsUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
}
