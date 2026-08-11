package com.ddtask.scheduler

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ddtask.scheduler.databinding.ActivityMainBinding
import com.ddtask.scheduler.databinding.DialogAddTaskBinding
import com.ddtask.scheduler.databinding.ItemTaskBinding
import com.ddtask.scheduler.model.ScheduledTask
import com.ddtask.scheduler.service.AlarmScheduler
import com.ddtask.scheduler.util.BrightnessController
import com.ddtask.scheduler.util.DingTalkLauncher
import com.ddtask.scheduler.util.KeepScreenOverlay
import com.ddtask.scheduler.util.SettingsStorage
import com.ddtask.scheduler.util.TaskStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var taskStorage: TaskStorage
    private lateinit var settingsStorage: SettingsStorage
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskStorage = TaskStorage(this)
        settingsStorage = SettingsStorage(this)
        alarmScheduler = AlarmScheduler(this)

        setupSettings()
        setupRecyclerView()
        setupFab()
        updateDingTalkStatus()
        requestExactAlarmPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshTaskList()
        updateDingTalkStatus()
        syncSettingsUi()
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

        if (settingsStorage.dimScreen && BrightnessController.canWriteSettings(this)) {
            BrightnessController.setMinimumBrightness(this)
        }
    }

    private fun handleKeepScreenOnToggle(enabled: Boolean) {
        if (!enabled) {
            settingsStorage.keepScreenOn = false
            return
        }
        val hasWriteSettings = BrightnessController.canWriteSettings(this)
        val hasOverlay = KeepScreenOverlay(this).canDrawOverlay()
        if (hasWriteSettings && hasOverlay) {
            settingsStorage.keepScreenOn = true
            return
        }
        binding.switchKeepScreenOn.isChecked = false
        showKeepScreenOnPermissionDialog(!hasWriteSettings, !hasOverlay)
    }

    private fun showKeepScreenOnPermissionDialog(needWriteSettings: Boolean, needOverlay: Boolean) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.keep_screen_on_permission_title)
            .setMessage(R.string.keep_screen_on_permission_message)
            .setPositiveButton(R.string.go_settings) { _, _ ->
                if (needWriteSettings) {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
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
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun handleDimScreenToggle(enabled: Boolean) {
        if (enabled) {
            if (!BrightnessController.canWriteSettings(this)) {
                binding.switchDimScreen.isChecked = false
                showWriteSettingsDialog()
                return
            }
            BrightnessController.saveCurrentBrightness(this, settingsStorage)
            BrightnessController.setMinimumBrightness(this)
            settingsStorage.dimScreen = true
        } else {
            settingsStorage.dimScreen = false
            BrightnessController.restoreBrightness(this, settingsStorage)
        }
    }

    private fun showWriteSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.write_settings_title)
            .setMessage(R.string.write_settings_message)
            .setPositiveButton(R.string.go_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            onEdit = { task -> showTaskDialog(task) },
            onToggle = { task, enabled ->
                val updated = task.copy(enabled = enabled)
                taskStorage.update(updated)
                if (enabled) {
                    alarmScheduler.schedule(updated)
                } else {
                    alarmScheduler.cancel(task.id)
                }
                refreshTaskList()
            },
            onDelete = { task ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_task)
                    .setMessage(getString(R.string.delete_task_confirm, task.timeText()))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        alarmScheduler.cancel(task.id)
                        taskStorage.delete(task.id)
                        refreshTaskList()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
        binding.recyclerTasks.layoutManager = LinearLayoutManager(this)
        binding.recyclerTasks.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { showTaskDialog(null) }
    }

    private fun refreshTaskList() {
        val tasks = taskStorage.getAll().sortedBy { it.hour * 60 + it.minute }
        adapter.submitList(tasks)
        binding.emptyView.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateDingTalkStatus() {
        val installed = DingTalkLauncher.isInstalled(this)
        binding.statusDingtalk.text = if (installed) {
            getString(R.string.dingtalk_installed)
        } else {
            getString(R.string.dingtalk_not_installed)
        }
        binding.statusDingtalk.setTextColor(
            getColor(if (installed) R.color.status_ok else R.color.status_error)
        )
    }

    private fun showTaskDialog(existingTask: ScheduledTask?) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        var selectedHour = existingTask?.hour ?: 9
        var selectedMinute = existingTask?.minute ?: 0
        val isEdit = existingTask != null

        if (isEdit) {
            dialogBinding.etLabel.setText(existingTask!!.label)
            dialogBinding.switchRepeatDaily.isChecked = existingTask.repeatDaily
        }

        dialogBinding.tvSelectedTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
        dialogBinding.btnPickTime.setOnClickListener {
            showTimePicker(selectedHour, selectedMinute) { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                dialogBinding.tvSelectedTime.text =
                    String.format("%02d:%02d", selectedHour, selectedMinute)
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) R.string.edit_task else R.string.add_task)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val label = dialogBinding.etLabel.text?.toString()?.trim().orEmpty()
                val repeatDaily = dialogBinding.switchRepeatDaily.isChecked

                if (isEdit) {
                    alarmScheduler.cancel(existingTask!!.id)
                    val updated = existingTask.copy(
                        hour = selectedHour,
                        minute = selectedMinute,
                        label = label,
                        repeatDaily = repeatDaily
                    )
                    taskStorage.update(updated)
                    if (updated.enabled) {
                        alarmScheduler.schedule(updated)
                    }
                } else {
                    val task = ScheduledTask(
                        id = taskStorage.nextId(),
                        hour = selectedHour,
                        minute = selectedMinute,
                        label = label,
                        enabled = true,
                        repeatDaily = repeatDaily
                    )
                    taskStorage.add(task)
                    alarmScheduler.schedule(task)
                }
                refreshTaskList()
                Toast.makeText(this, R.string.task_saved, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showTimePicker(hour: Int, minute: Int, onSelected: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText(getString(R.string.pick_time))
            .build()
        picker.addOnPositiveButtonClickListener {
            onSelected(picker.hour, picker.minute)
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.exact_alarm_title)
                    .setMessage(R.string.exact_alarm_message)
                    .setPositiveButton(R.string.go_settings) { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        })
                    }
                    .setNegativeButton(R.string.later, null)
                    .show()
            }
        }
    }
}

private class TaskAdapter(
    private val onEdit: (ScheduledTask) -> Unit,
    private val onToggle: (ScheduledTask, Boolean) -> Unit,
    private val onDelete: (ScheduledTask) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks: List<ScheduledTask> = emptyList()

    fun submitList(newTasks: List<ScheduledTask>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: ScheduledTask) {
            binding.tvTime.text = task.timeText()
            binding.tvLabel.text = if (task.label.isNotEmpty()) {
                task.label
            } else {
                binding.root.context.getString(R.string.open_dingtalk)
            }
            binding.tvRepeat.text = if (task.repeatDaily) {
                binding.root.context.getString(R.string.repeat_daily)
            } else {
                binding.root.context.getString(R.string.once)
            }
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = task.enabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(task, isChecked)
            }
            binding.tvTime.setOnClickListener { onEdit(task) }
            binding.btnDelete.setOnClickListener { onDelete(task) }
        }
    }
}
