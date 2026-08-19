package com.ddtask.scheduler.util

/** 根据 SMTP 配置推断 IMAP 服务器地址。 */
object ImapHelper {

    private const val DEFAULT_IMAP_PORT = 993

    fun resolveHost(smtpHost: String): String {
        val host = smtpHost.trim().lowercase()
        if (host.isEmpty()) return "imap.qq.com"
        return when {
            host.startsWith("imap.") -> host
            host.startsWith("smtp.") -> "imap." + host.removePrefix("smtp.")
            else -> "imap.$host"
        }
    }

    fun defaultPort(): Int = DEFAULT_IMAP_PORT
}
