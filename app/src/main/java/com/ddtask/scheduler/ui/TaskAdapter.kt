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
        notifyItemChanged(index, PAYLOAD_TOGGLE)
    }

    override fun getItemId(position: Int): Long = tasks[position].id

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

        /** 当前行绑定的任务 id，开关回调只认此 id，避免列表位置错位。 */
        private var boundTaskId: Long = NO_TASK_ID
        private var suppressToggleCallback = false

        init {
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                if (suppressToggleCallback) return@setOnCheckedChangeListener
                val task = tasks.find { it.id == boundTaskId } ?: return@setOnCheckedChangeListener
                if (task.enabled == isChecked) return@setOnCheckedChangeListener
                onToggle(task, isChecked)
            }
            binding.taskContent.setOnClickListener {
                val task = tasks.find { it.id == boundTaskId } ?: return@setOnClickListener
                onEdit(task)
            }
            binding.btnDelete.setOnClickListener {
                val task = tasks.find { it.id == boundTaskId } ?: return@setOnClickListener
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
    }
}
