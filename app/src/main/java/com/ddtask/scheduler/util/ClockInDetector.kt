package com.ddtask.scheduler.util

object ClockInDetector {

    private val SUCCESS_KEYWORDS = listOf(
        "极速打卡成功",
        "打卡成功",
        "上班打卡成功",
        "下班打卡成功"
    )

    const val DINGTALK_PACKAGE = "com.alibaba.android.rimet"

    fun isClockInSuccess(text: String): Boolean {
        if (text.isBlank()) return false
        return SUCCESS_KEYWORDS.any { keyword -> text.contains(keyword) }
    }
}
