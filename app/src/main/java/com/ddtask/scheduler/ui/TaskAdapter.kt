package com.ddtask.scheduler.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.ItemTaskBinding
import com.ddtask.scheduler.model.ScheduledTask

/** 任务列表 RecyclerView：展示时间/备注/重复方式，支持开关、编辑、删除。 */
class TaskAdapter(
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
            binding.tvRepeat.text = task.repeatText(binding.root.context.resources)
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = task.enabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(task, isChecked)
            }
            binding.taskContent.setOnClickListener { onEdit(task) }
            binding.btnDelete.setOnClickListener { onDelete(task) }
        }
    }
}
