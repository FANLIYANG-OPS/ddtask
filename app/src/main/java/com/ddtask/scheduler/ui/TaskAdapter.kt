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

    fun updateTask(updated: ScheduledTask) {
        val index = tasks.indexOfFirst { it.id == updated.id }
        if (index < 0) return
        tasks = tasks.toMutableList().apply { set(index, updated) }
        notifyItemChanged(index, PAYLOAD_TOGGLE)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_TOGGLE)) {
            holder.bindToggle(tasks[position])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = tasks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: ScheduledTask) {
            binding.root.tag = task.id
            binding.tvTime.text = task.timeText()
            binding.tvLabel.text = if (task.label.isNotEmpty()) {
                task.label
            } else {
                binding.root.context.getString(R.string.open_dingtalk)
            }
            binding.tvRepeat.text = task.repeatText(binding.root.context.resources)
            bindToggle(task)
            binding.taskContent.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onEdit(tasks[pos])
            }
            binding.btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onDelete(tasks[pos])
            }
        }

        fun bindToggle(task: ScheduledTask) {
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = task.enabled
            binding.switchEnabled.jumpDrawablesToCurrentState()
            binding.switchEnabled.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val current = tasks.getOrNull(pos) ?: return@setOnClickListener
                if (binding.root.tag != current.id) return@setOnClickListener
                onToggle(current, binding.switchEnabled.isChecked)
            }
        }
    }

    companion object {
        private const val PAYLOAD_TOGGLE = "toggle"
    }
}
