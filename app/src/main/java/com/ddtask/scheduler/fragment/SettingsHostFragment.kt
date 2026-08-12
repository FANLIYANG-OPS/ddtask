package com.ddtask.scheduler.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.FragmentSettingsHostBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

class SettingsHostFragment : Fragment() {

    private var _binding: FragmentSettingsHostBinding? = null
    private val binding get() = _binding!!

    private var sidebarExpanded = true
    private var currentSection = SECTION_NOTIFICATION

    private var notificationFragment: NotificationSettingsFragment? = null
    private var emailFragment: EmailSettingsFragment? = null
    private var backupFragment: BackupSettingsFragment? = null

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
        if (savedInstanceState != null) {
            sidebarExpanded = savedInstanceState.getBoolean(KEY_SIDEBAR_EXPANDED, true)
            currentSection = savedInstanceState.getString(KEY_SECTION, SECTION_NOTIFICATION)
                ?: SECTION_NOTIFICATION
        }
        setupSidebar()
        ensureChildFragments()
        showSection(currentSection)
        updateSidebarUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SIDEBAR_EXPANDED, sidebarExpanded)
        outState.putString(KEY_SECTION, currentSection)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showSection(section: String) {
        currentSection = section
        sidebarExpanded = true
        updateSidebarUi()
        ensureChildFragments()
        val transaction = childFragmentManager.beginTransaction()
        notificationFragment?.let { transaction.hide(it) }
        emailFragment?.let { transaction.hide(it) }
        backupFragment?.let { transaction.hide(it) }
        when (section) {
            SECTION_EMAIL -> emailFragment?.let { transaction.show(it) }
            SECTION_BACKUP -> backupFragment?.let { transaction.show(it) }
            else -> notificationFragment?.let { transaction.show(it) }
        }
        transaction.commit()
        highlightNavItem(section)
    }

    private fun setupSidebar() {
        binding.btnToggleSidebar.setOnClickListener {
            sidebarExpanded = !sidebarExpanded
            updateSidebarUi()
        }
        binding.navNotificationSettings.setOnClickListener {
            showSection(SECTION_NOTIFICATION)
        }
        binding.navEmailSettings.setOnClickListener {
            showSection(SECTION_EMAIL)
        }
        binding.navBackupSettings.setOnClickListener {
            showSection(SECTION_BACKUP)
        }
    }

    private fun ensureChildFragments() {
        if (notificationFragment == null) {
            notificationFragment = NotificationSettingsFragment()
            emailFragment = EmailSettingsFragment()
            backupFragment = BackupSettingsFragment()
            childFragmentManager.beginTransaction()
                .add(R.id.settings_content, notificationFragment!!, TAG_NOTIFICATION)
                .add(R.id.settings_content, emailFragment!!, TAG_EMAIL)
                .add(R.id.settings_content, backupFragment!!, TAG_BACKUP)
                .hide(emailFragment!!)
                .hide(backupFragment!!)
                .commitNow()
        }
    }

    private fun updateSidebarUi() {
        val sidebarWidth = if (sidebarExpanded) {
            resources.getDimensionPixelSize(R.dimen.settings_sidebar_expanded)
        } else {
            resources.getDimensionPixelSize(R.dimen.settings_sidebar_collapsed)
        }
        binding.settingsSidebar.layoutParams = binding.settingsSidebar.layoutParams.apply {
            width = sidebarWidth
        }

        binding.btnToggleSidebar.text = getString(
            if (sidebarExpanded) R.string.settings_sidebar_collapse else R.string.settings_sidebar_expand
        )

        val navVisibility = if (sidebarExpanded) View.VISIBLE else View.GONE
        binding.navNotificationSettings.visibility = navVisibility
        binding.navEmailSettings.visibility = navVisibility
        binding.navBackupSettings.visibility = navVisibility
    }

    private fun highlightNavItem(section: String) {
        styleNavItem(binding.navNotificationSettings, section == SECTION_NOTIFICATION)
        styleNavItem(binding.navEmailSettings, section == SECTION_EMAIL)
        styleNavItem(binding.navBackupSettings, section == SECTION_BACKUP)
    }

    private fun styleNavItem(button: MaterialButton, selected: Boolean) {
        button.setTextColor(
            if (selected) {
                ContextCompat.getColor(requireContext(), R.color.status_ok)
            } else {
                MaterialColors.getColor(button, com.google.android.material.R.attr.colorOnSurface)
            }
        )
        button.alpha = if (selected) 1f else 0.7f
    }

    companion object {
        const val SECTION_NOTIFICATION = "notification"
        const val SECTION_EMAIL = "email"
        const val SECTION_BACKUP = "backup"

        private const val KEY_SIDEBAR_EXPANDED = "sidebar_expanded"
        private const val KEY_SECTION = "settings_section"
        private const val TAG_NOTIFICATION = "settings_notification"
        private const val TAG_EMAIL = "settings_email"
        private const val TAG_BACKUP = "settings_backup"
    }
}
