package com.ddtask.scheduler.util

/** 打卡相关关键字的默认值、解析与模糊匹配。 */
object ClockInDetector {

    const val DINGTALK_PACKAGE = "com.alibaba.android.rimet"

    val DEFAULT_TRIGGER_KEYWORDS = listOf(
        "上班打卡",
        "下班打卡"
    )

    val DEFAULT_SUCCESS_KEYWORDS = listOf(
        "极速打卡成功",
        "打卡成功",
        "上班打卡成功",
        "下班打卡成功"
    )

    /** 返回 DDTask 的默认关键字与打卡成功相同，可在设置中单独配置。 */
    val DEFAULT_RETURN_KEYWORDS = DEFAULT_SUCCESS_KEYWORDS

    fun defaultTriggerKeywordsText(): String = DEFAULT_TRIGGER_KEYWORDS.joinToString("\n")

    fun defaultSuccessKeywordsText(): String = DEFAULT_SUCCESS_KEYWORDS.joinToString("\n")

    fun defaultReturnKeywordsText(): String = DEFAULT_RETURN_KEYWORDS.joinToString("\n")

    /** 支持换行、逗号、分号分隔；空输入时回退到 [fallback]。 */
    fun parseKeywords(raw: String, fallback: List<String> = emptyList()): List<String> {
        val parsed = raw.split('\n', ',', '，', ';', '；')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return parsed.ifEmpty { fallback }
    }

    fun matchesAny(text: String, keywords: List<String>): Boolean {
        if (text.isBlank() || keywords.isEmpty()) return false
        return keywords.any { keyword -> text.contains(keyword) }
    }

    fun matchesTrigger(text: String, rawKeywords: String): Boolean {
        return matchesAny(
            text,
            parseKeywords(rawKeywords, DEFAULT_TRIGGER_KEYWORDS)
        )
    }

    fun matchesSuccess(text: String, rawKeywords: String): Boolean {
        return matchesAny(
            text,
            parseKeywords(rawKeywords, DEFAULT_SUCCESS_KEYWORDS)
        )
    }

    fun matchesReturn(text: String, returnRaw: String, successRaw: String): Boolean {
        val fallback = parseKeywords(successRaw, DEFAULT_SUCCESS_KEYWORDS)
        return matchesAny(text, parseKeywords(returnRaw, fallback))
    }
}
