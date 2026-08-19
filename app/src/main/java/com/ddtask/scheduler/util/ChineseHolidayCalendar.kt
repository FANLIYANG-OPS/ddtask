package com.ddtask.scheduler.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 中国法定节假日与调休工作日（依据国务院办公厅通知）。
 * 数据来源：assets/chinese_holidays.json
 */
class ChineseHolidayCalendar private constructor(
    private val holidays: Set<String>,
    private val extraWorkdays: Set<String>,
    private val coveredYears: Set<Int>
) {

    fun isWorkday(calendar: Calendar): Boolean {
        val dateKey = formatDate(calendar)
        val year = calendar.get(Calendar.YEAR)

        if (year !in coveredYears) {
            return isWeekdayFallback(calendar)
        }
        if (dateKey in holidays) return false
        if (dateKey in extraWorkdays) return true
        return isWeekdayFallback(calendar)
    }

    fun hasDataForYear(year: Int): Boolean = year in coveredYears

    fun coveredYears(): Set<Int> = coveredYears

    private fun isWeekdayFallback(calendar: Calendar): Boolean {
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return day in Calendar.MONDAY..Calendar.FRIDAY
    }

    private fun formatDate(calendar: Calendar): String {
        return DATE_FORMAT.format(calendar.time)
    }

    private data class HolidayData(
        @SerializedName("years") val years: Map<String, YearData> = emptyMap()
    )

    private data class YearData(
        @SerializedName("holidays") val holidays: List<List<String>> = emptyList(),
        @SerializedName("extraWorkdays") val extraWorkdays: List<String> = emptyList()
    )

    companion object {
        private val DATE_FORMAT = SimpleDateFormat(DateFormats.DATE_ONLY, Locale.US)
        private const val ASSET_HOLIDAYS_JSON = "chinese_holidays.json"
        private const val HOLIDAY_DATE_RANGE_SIZE = 2

        @Volatile
        private var instance: ChineseHolidayCalendar? = null

        fun getInstance(context: Context): ChineseHolidayCalendar {
            return instance ?: synchronized(this) {
                instance ?: load(context.applicationContext).also { instance = it }
            }
        }

        private fun load(context: Context): ChineseHolidayCalendar {
            val json = context.assets.open(ASSET_HOLIDAYS_JSON)
                .bufferedReader()
                .use { it.readText() }
            val data = Gson().fromJson(json, HolidayData::class.java)

            val holidaySet = mutableSetOf<String>()
            val workdaySet = mutableSetOf<String>()
            val years = mutableSetOf<Int>()

            data.years.forEach { (yearStr, yearData) ->
                years.add(yearStr.toInt())
                yearData.holidays.forEach { range ->
                    if (range.size == HOLIDAY_DATE_RANGE_SIZE) {
                        holidaySet.addAll(expandDateRange(range[0], range[1]))
                    }
                }
                workdaySet.addAll(yearData.extraWorkdays)
            }

            return ChineseHolidayCalendar(holidaySet, workdaySet, years)
        }

        private fun expandDateRange(start: String, end: String): List<String> {
            val startCal = parseDate(start)
            val endCal = parseDate(end)
            val result = mutableListOf<String>()
            val cursor = startCal.clone() as Calendar
            while (!cursor.after(endCal)) {
                result.add(DATE_FORMAT.format(cursor.time))
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
            return result
        }

        private fun parseDate(date: String): Calendar {
            val cal = Calendar.getInstance()
            val parsed = DATE_FORMAT.parse(date) ?: error("Invalid date: $date")
            cal.time = parsed
            resetTime(cal)
            return cal
        }

        private fun resetTime(calendar: Calendar) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
    }
}
