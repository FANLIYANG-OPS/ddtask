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

    init {
        setHasStableIds(true)
    }

    fun submitList(newTasks: List<ScheduledTask>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    fun updateTask(updated: ScheduledTask) {
        val index = tasks.indexOfFirst { it.id == updated.id }
        if (index < 0) return
        tasks = tasks.toMutableList().apply { set(index, updated) }
        notifyItemChanged(index)
    }

    override fun getItemId(position: Int): Long = tasks[position].id

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
            binding.switchEnabled.jumpDrawablesToCurrentState()
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener
                val current = tasks.getOrNull(pos) ?: return@setOnCheckedChangeListener
                if (current.enabled == isChecked) return@setOnCheckedChangeListener
                onToggle(current, isChecked)
            }
            binding.taskContent.setOnClickListener { onEdit(task) }
            binding.btnDelete.setOnClickListener { onDelete(task) }
        }
    }
}
