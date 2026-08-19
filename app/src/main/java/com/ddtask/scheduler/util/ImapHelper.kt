package com.ddtask.scheduler.util

/** 根据 SMTP 配置推断 IMAP 服务器地址。 */
object ImapHelper {

    private const val IMAP_PREFIX = "imap."
    private const val SMTP_PREFIX = "smtp."

    private val NETEASE_DOMAIN_SUFFIXES = listOf(
        ".163.com",
        ".126.com",
        ".188.com",
        ".yeah.net"
    )

    fun resolveHost(smtpHost: String): String {
        val host = smtpHost.trim().lowercase()
        if (host.isEmpty()) return EmailDefaults.DEFAULT_IMAP_HOST
        return when {
            host.startsWith(IMAP_PREFIX) -> host
            host.startsWith(SMTP_PREFIX) -> IMAP_PREFIX + host.removePrefix(SMTP_PREFIX)
            else -> "$IMAP_PREFIX$host"
        }
    }

    fun defaultPort(): Int = EmailDefaults.DEFAULT_IMAP_PORT

    /** 网易系邮箱要求连接后发送 IMAP ID，否则会报 Unsafe Login。 */
    fun requiresImapId(host: String): Boolean {
        val h = host.trim().lowercase()
        return NETEASE_DOMAIN_SUFFIXES.any { h.endsWith(it) }
    }
}
