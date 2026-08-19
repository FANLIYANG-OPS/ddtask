package com.ddtask.scheduler.util

import android.content.Context

/** 打卡动作来源：系统通知或邮件。 */
enum class ClockInSource {
    NOTIFICATION,
    EMAIL
}

/**
 * 统一处理「打开钉钉 / 返回 DDTask / 打卡成功」逻辑，
 * 供通知监听与邮件轮询共用。
 */
class ClockInActionHandler(private val context: Context) {

    private val appContext = context.applicationContext
    private val storage = NotificationStorage(appContext)

    fun handleIncomingText(
        text: String,
        source: ClockInSource,
        packageName: String? = null
    ) {
        if (text.isBlank()) return
        handleAutoOpenDingTalk(text, source)
        handleReturnToApp(text, source, packageName)
        handleClockInSuccess(source, packageName, text)
    }

    private fun handleAutoOpenDingTalk(text: String, source: ClockInSource) {
        when (source) {
            ClockInSource.NOTIFICATION -> if (!storage.autoOpenDingTalkEnabled) return
            ClockInSource.EMAIL -> if (!storage.emailTriggerEnabled) return
        }
        if (!ClockInDetector.matchesTrigger(text, storage.triggerKeywords)) return
        if (storage.shouldSkipDuplicateOpen()) return
        if (!DingTalkLauncher.launch(appContext)) return
        storage.recordOpenDingTalk(text)
        ClockInSessionManager(appContext).startSession(text)
    }

    private fun handleReturnToApp(text: String, source: ClockInSource, packageName: String?) {
        if (!storage.closeDingTalkEnabled) return

        if (ClockInDetector.matchesReturn(text, storage.returnKeywords)) {
            if (storage.shouldSkipDuplicateReturn()) return
            storage.recordReturnTriggered()
            AppNavigator.goToMain(appContext)
            return
        }

        if (source != ClockInSource.NOTIFICATION) return
        val sessionManager = ClockInSessionManager(appContext)
        if (!sessionManager.isActive()) return
        val matchesDingTalkSuccess = packageName == ClockInDetector.DINGTALK_PACKAGE &&
            ClockInDetector.matchesSuccess(text, storage.successKeywords)
        if (!matchesDingTalkSuccess) return
        sessionManager.onReturnKeywordMatched(text)
    }

    private fun handleClockInSuccess(source: ClockInSource, packageName: String?, text: String) {
        if (source == ClockInSource.NOTIFICATION && packageName != ClockInDetector.DINGTALK_PACKAGE) {
            return
        }
        if (!ClockInDetector.matchesSuccess(text, storage.successKeywords)) return

        val sessionManager = ClockInSessionManager(appContext)
        if (sessionManager.isActive()) {
            sessionManager.onClockInSuccess(text)
            return
        }

        if (source == ClockInSource.EMAIL) return
        if (!storage.emailNotifyEnabled || !storage.isConfigured()) return
        if (storage.shouldSkipDuplicateEmail()) return
        EmailSender.sendClockInSuccess(appContext, text) { success, _ ->
            if (success) storage.recordEmailSent(text)
        }
    }
}
