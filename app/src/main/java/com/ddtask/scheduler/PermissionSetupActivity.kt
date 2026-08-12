package com.ddtask.scheduler

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import com.ddtask.scheduler.databinding.ActivityPermissionSetupBinding
import com.ddtask.scheduler.databinding.ItemPermissionStepBinding
import com.ddtask.scheduler.util.PermissionSetupHelper
import com.ddtask.scheduler.util.SettingsStorage
import com.ddtask.scheduler.R

class PermissionSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionSetupBinding
    private lateinit var settingsStorage: SettingsStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsStorage = SettingsStorage(this)
        binding.toolbar.setNavigationOnClickListener { finishSetup() }
        binding.btnEnterApp.setOnClickListener { finishSetup() }
        renderSteps()
    }

    override fun onResume() {
        super.onResume()
        renderSteps()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionSetupHelper.REQUEST_POST_NOTIFICATIONS) {
            renderSteps()
        }
    }

    private fun renderSteps() {
        val steps = PermissionSetupHelper.requiredSteps(this)
        binding.tvProgress.text = getString(
            R.string.permission_setup_progress,
            PermissionSetupHelper.grantedCount(this),
            PermissionSetupHelper.totalCount(this)
        )

        binding.layoutSteps.removeAllViews()
        val inflater = LayoutInflater.from(this)
        steps.forEach { step ->
            val itemBinding = ItemPermissionStepBinding.inflate(inflater, binding.layoutSteps, false)
            val granted = step.isGranted(this)
            itemBinding.tvStepTitle.setText(step.titleRes)
            itemBinding.tvStepMessage.setText(step.messageRes)
            itemBinding.tvStepStatus.text = getString(
                if (granted) R.string.permission_granted else R.string.permission_pending
            )
            itemBinding.tvStepStatus.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (granted) R.color.status_ok else R.color.status_error
                )
            )
            itemBinding.btnGrant.visibility = if (granted) View.GONE else View.VISIBLE
            itemBinding.btnGrant.setOnClickListener {
                PermissionSetupHelper.openStepSettings(this, step.id)
            }
            binding.layoutSteps.addView(itemBinding.root)
        }
    }

    private fun finishSetup() {
        settingsStorage.permissionSetupCompleted = true
        setResult(RESULT_OK)
        finish()
    }
}
