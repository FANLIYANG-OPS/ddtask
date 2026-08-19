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

    /** 网易系邮箱要求连接后发送 IMAP ID，否则会报 Unsafe Login。 */
    fun requiresImapId(host: String): Boolean {
        val h = host.trim().lowercase()
        return h.endsWith(".163.com") ||
            h.endsWith(".126.com") ||
            h.endsWith(".188.com") ||
            h.endsWith(".yeah.net")
    }
}
