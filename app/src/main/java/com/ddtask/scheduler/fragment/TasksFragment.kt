package com.ddtask.scheduler.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.DialogAddTaskBinding
import com.ddtask.scheduler.databinding.FragmentTasksBinding
import com.ddtask.scheduler.model.RepeatMode
import com.ddtask.scheduler.model.ScheduledTask
import com.ddtask.scheduler.model.TaskTemplate
import com.ddtask.scheduler.ui.TaskAdapter
import com.ddtask.scheduler.util.ExactAlarmHelper
import com.ddtask.scheduler.util.NewTaskSpec
import com.ddtask.scheduler.util.ScheduleCalculator
import com.ddtask.scheduler.util.TaskRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

/** 任务 Tab：列表展示、添加/编辑/删除定时任务。 */
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskRepository: TaskRepository
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskRepository = TaskRepository(requireContext())
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        refreshTaskList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showAddTaskDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_task_template)
            .setItems(
                arrayOf(
                    getString(R.string.template_custom),
                    getString(R.string.template_summer),
                    getString(R.string.template_winter)
                )
            ) { _, which ->
                when (which) {
                    TEMPLATE_INDEX_CUSTOM -> showTaskDialog(null)
                    TEMPLATE_INDEX_SUMMER -> applyTemplate(TaskTemplate.SUMMER, R.string.template_summer)
                    TEMPLATE_INDEX_WINTER -> applyTemplate(TaskTemplate.WINTER, R.string.template_winter)
                }
            }
            .show()
    }

    /** 模板：一次写入两条独立任务，上班/下班除创建时机外无任何关联。 */
    private fun applyTemplate(template: TaskTemplate, nameRes: Int) {
        taskRepository.createIndependent(template.toTaskSpecs())
        refreshTaskList()
        Toast.makeText(
            requireContext(),
            getString(R.string.template_applied, getString(nameRes)),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            onEdit = { task -> showTaskDialog(task) },
            onToggle = { task, enabled -> handleToggle(task, enabled) },
            onDelete = { task -> confirmDelete(task) }
        )
        binding.recyclerTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTasks.adapter = adapter
    }

    private fun handleToggle(task: ScheduledTask, enabled: Boolean) {
        if (!isAdded) return
        val result = taskRepository.setEnabled(task, enabled)
        binding.recyclerTasks.post {
            if (!isAdded) return@post
            refreshTaskList()
            if (result.scheduleFailed && !ExactAlarmHelper.canScheduleExactAlarms(requireContext())) {
                Toast.makeText(
                    requireContext(),
                    R.string.exact_alarm_message,
                    Toast.LENGTH_LONG
                ).show()
                try {
                    ExactAlarmHelper.openSettings(requireContext())
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun confirmDelete(task: ScheduledTask) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_task)
            .setMessage(getString(R.string.delete_task_confirm, task.timeText()))
            .setPositiveButton(R.string.delete) { _, _ ->
                taskRepository.delete(task.id)
                refreshTaskList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun refreshTaskList() {
        val tasks = taskRepository.list()
        adapter.submitTasks(tasks)
        binding.emptyView.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        binding.hintEdit.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showTaskDialog(existingTask: ScheduledTask?) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        var selectedHour = existingTask?.hour ?: DEFAULT_TASK_HOUR
        var selectedMinute = existingTask?.minute ?: DEFAULT_TASK_MINUTE
        val isEdit = existingTask != null

        val repeatModeOptions = listOf(
            RepeatMode.ONCE to getString(R.string.repeat_once),
            RepeatMode.DAILY to getString(R.string.repeat_daily),
            RepeatMode.WEEKDAYS to getString(R.string.repeat_weekdays),
            RepeatMode.CRON to getString(R.string.repeat_cron)
        )
        var selectedMode = existingTask?.effectiveMode() ?: RepeatMode.DAILY

        dialogBinding.dropdownRepeatMode.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                repeatModeOptions.map { it.second }
            )
        )
        dialogBinding.dropdownRepeatMode.setText(
            repeatModeOptions.first { it.first == selectedMode }.second,
            false
        )

        if (isEdit) {
            dialogBinding.etLabel.setText(existingTask!!.label)
            if (existingTask.effectiveMode() == RepeatMode.CRON) {
                dialogBinding.etCron.setText(existingTask.cronExpression)
            }
        }

        fun updateRepeatUi() {
            val isCron = selectedMode == RepeatMode.CRON
            dialogBinding.layoutTime.visibility = if (isCron) View.GONE else View.VISIBLE
            dialogBinding.layoutCron.visibility = if (isCron) View.VISIBLE else View.GONE
            dialogBinding.tvCronHelp.visibility = if (isCron) View.VISIBLE else View.GONE
            if (isCron && dialogBinding.etCron.text.isNullOrBlank()) {
                dialogBinding.etCron.setText(
                    ScheduleCalculator.cronFromTime(selectedHour, selectedMinute)
                )
            }
        }

        dialogBinding.dropdownRepeatMode.setOnItemClickListener { _, _, position, _ ->
            selectedMode = repeatModeOptions[position].first
            updateRepeatUi()
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
        updateRepeatUi()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEdit) R.string.edit_task else R.string.add_task)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val maxHeight = (resources.displayMetrics.heightPixels * DIALOG_MAX_HEIGHT_RATIO).toInt()
            dialogBinding.root.post {
                if (dialogBinding.root.height > maxHeight) {
                    dialogBinding.root.layoutParams.height = maxHeight
                    dialogBinding.root.requestLayout()
                }
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                saveTaskFromDialog(
                    isEdit = isEdit,
                    existingTask = existingTask,
                    label = dialogBinding.etLabel.text?.toString()?.trim().orEmpty(),
                    hour = selectedHour,
                    minute = selectedMinute,
                    mode = selectedMode,
                    cronExpression = if (selectedMode == RepeatMode.CRON) {
                        dialogBinding.etCron.text?.toString()?.trim().orEmpty()
                    } else {
                        ""
                    }
                )?.let {
                    refreshTaskList()
                    Toast.makeText(requireContext(), R.string.task_saved, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun saveTaskFromDialog(
        isEdit: Boolean,
        existingTask: ScheduledTask?,
        label: String,
        hour: Int,
        minute: Int,
        mode: RepeatMode,
        cronExpression: String
    ): ScheduledTask? {
        if (mode == RepeatMode.CRON && !ScheduleCalculator.isValidCron(cronExpression)) {
            Toast.makeText(requireContext(), R.string.cron_invalid, Toast.LENGTH_SHORT).show()
            return null
        }

        val spec = NewTaskSpec(
            hour = hour,
            minute = minute,
            label = label,
            enabled = existingTask?.enabled ?: true,
            repeatMode = mode,
            cronExpression = cronExpression
        )

        return if (isEdit && existingTask != null) {
            taskRepository.update(
                spec.toScheduledTask(existingTask.id).copy(enabled = existingTask.enabled)
            )
        } else {
            taskRepository.create(spec)
        }
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
        picker.show(parentFragmentManager, TAG_TIME_PICKER)
    }

    companion object {
        private const val DEFAULT_TASK_HOUR = 9
        private const val DEFAULT_TASK_MINUTE = 0
        private const val DIALOG_MAX_HEIGHT_RATIO = 0.75
        private const val TEMPLATE_INDEX_CUSTOM = 0
        private const val TEMPLATE_INDEX_SUMMER = 1
        private const val TEMPLATE_INDEX_WINTER = 2
        private const val TAG_TIME_PICKER = "time_picker"
    }
}
