package com.ddtask.scheduler.util

import android.content.Context
import com.ddtask.scheduler.model.ScheduledTask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** 定时任务列表的 Gson JSON 持久化（SharedPreferences）。 */
class TaskStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<ScheduledTask> {
        val json = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        val type = object : TypeToken<List<ScheduledTask>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveAll(tasks: List<ScheduledTask>) {
        prefs.edit().putString(KEY_TASKS, gson.toJson(tasks)).apply()
    }

    fun add(task: ScheduledTask) {
        val tasks = getAll().toMutableList()
        tasks.add(task)
        saveAll(tasks)
    }

    fun update(task: ScheduledTask) {
        val tasks = getAll().toMutableList()
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            tasks[index] = task
            saveAll(tasks)
        }
    }

    fun delete(id: Long) {
        saveAll(getAll().filter { it.id != id })
    }

    fun getById(id: Long): ScheduledTask? = getAll().find { it.id == id }

    fun nextId(): Long {
        val maxId = getAll().maxOfOrNull { it.id } ?: 0L
        return maxId + 1
    }

    companion object {
        private const val PREFS_NAME = "ddtask_prefs"
        private const val KEY_TASKS = "scheduled_tasks"
    }
}
