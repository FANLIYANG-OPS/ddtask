package com.ddtask.scheduler.util

import android.content.Context
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.ddtask.scheduler.model.RepeatMode
import com.ddtask.scheduler.model.ScheduledTask
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import kotlin.random.Random

/** 根据重复模式计算下次闹钟触发时间；工作日模式结合中国节假日日历，并加 ±60s 随机抖动。 */
class ScheduleCalculator(context: Context) {

    private val holidayCalendar = ChineseHolidayCalendar.getInstance(context)

    private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    private val cronParser = CronParser(cronDefinition)

    fun nextTriggerTime(task: ScheduledTask): Long? {
        val base = when (task.effectiveMode()) {
            RepeatMode.ONCE -> baseOnce(task.hour, task.minute)
            RepeatMode.DAILY -> baseDaily(task.hour, task.minute)
            RepeatMode.WEEKDAYS -> baseChineseWorkdays(task.hour, task.minute)
            RepeatMode.CRON -> baseCron(task.cronExpression)
        } ?: return null
        return applyJitter(base)
    }

    fun isValidCron(expression: String): Boolean {
        return try {
            cronParser.parse(expression.trim())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun cronFromTime(hour: Int, minute: Int): String = "$minute $hour * * *"

    private fun baseOnce(hour: Int, minute: Int): Long = baseDaily(hour, minute)

    private fun baseDaily(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun baseChineseWorkdays(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        repeat(90) {
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            if (calendar.timeInMillis > System.currentTimeMillis() &&
                holidayCalendar.isWorkday(calendar)
            ) {
                return calendar.timeInMillis
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return baseDaily(hour, minute)
    }

    private fun baseCron(expression: String): Long? {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val cron = cronParser.parse(trimmed)
            val executionTime = ExecutionTime.forCron(cron)
            val now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
            val next = executionTime.nextExecution(now).orElse(null) ?: return null
            next.toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    /** 在目标时刻附近随机偏移，避免多任务同一秒触发。 */
    private fun applyJitter(baseMs: Long): Long {
        val jitterMs = Random.nextInt(-JITTER_SECONDS, JITTER_SECONDS + 1) * 1000L
        val withJitter = baseMs + jitterMs
        return if (withJitter > System.currentTimeMillis()) {
            withJitter
        } else {
            baseMs + Random.nextInt(0, JITTER_SECONDS + 1) * 1000L
        }
    }

    companion object {
        private const val JITTER_SECONDS = 60

        fun cronFromTime(hour: Int, minute: Int): String = "$minute $hour * * *"

        fun isValidCron(expression: String): Boolean {
            return try {
                CronParser(
                    CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
                ).parse(expression.trim())
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
