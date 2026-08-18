package com.ddtask.scheduler.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ddtask.scheduler.R
import com.ddtask.scheduler.databinding.ItemTaskBinding
import com.ddtask.scheduler.model.ScheduledTask

/** 任务列表：每条任务以唯一 id 绑定，开关/编辑/删除互不影响。 */
class TaskAdapter(
    private val onEdit: (ScheduledTask) -> Unit,
    private val onToggle: (ScheduledTask, Boolean) -> Unit,
    private val onDelete: (ScheduledTask) -> Unit
) : ListAdapter<ScheduledTask, TaskAdapter.TaskViewHolder>(DIFF) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_TOGGLE)) {
            holder.bindToggle(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun submitTasks(tasks: List<ScheduledTask>) {
        submitList(tasks)
    }

    fun updateTask(updated: ScheduledTask) {
        val index = currentList.indexOfFirst { it.id == updated.id }
        if (index < 0) return
        notifyItemChanged(index, PAYLOAD_TOGGLE)
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundTaskId: Long = NO_TASK_ID
        private var suppressToggleCallback = false

        init {
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                if (suppressToggleCallback) return@setOnCheckedChangeListener
                val task = currentList.find { it.id == boundTaskId } ?: return@setOnCheckedChangeListener
                if (task.enabled == isChecked) return@setOnCheckedChangeListener
                onToggle(task, isChecked)
            }
            binding.taskContent.setOnClickListener {
                val task = currentList.find { it.id == boundTaskId } ?: return@setOnClickListener
                onEdit(task)
            }
            binding.btnDelete.setOnClickListener {
                val task = currentList.find { it.id == boundTaskId } ?: return@setOnClickListener
                onDelete(task)
            }
        }

        fun bind(task: ScheduledTask) {
            boundTaskId = task.id
            binding.tvTime.text = task.timeText()
            binding.tvLabel.text = if (task.label.isNotEmpty()) {
                task.label
            } else {
                binding.root.context.getString(R.string.open_dingtalk)
            }
            binding.tvRepeat.text = task.repeatText(binding.root.context.resources)
            bindToggle(task)
        }

        fun bindToggle(task: ScheduledTask) {
            boundTaskId = task.id
            suppressToggleCallback = true
            binding.switchEnabled.isChecked = task.enabled
            binding.switchEnabled.jumpDrawablesToCurrentState()
            suppressToggleCallback = false
        }
    }

    companion object {
        private const val PAYLOAD_TOGGLE = "toggle"
        private const val NO_TASK_ID = -1L

        private val DIFF = object : DiffUtil.ItemCallback<ScheduledTask>() {
            override fun areItemsTheSame(oldItem: ScheduledTask, newItem: ScheduledTask): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ScheduledTask, newItem: ScheduledTask): Boolean {
                return oldItem == newItem
            }
        }
    }
}
