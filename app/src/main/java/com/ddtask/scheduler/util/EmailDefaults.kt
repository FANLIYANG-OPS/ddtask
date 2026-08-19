package com.ddtask.scheduler.util

/** 邮件/IMAP 默认主机、端口及合法端口范围。 */
object EmailDefaults {
    const val DEFAULT_SMTP_HOST = "smtp.qq.com"
    const val DEFAULT_SMTP_PORT = 465
    const val DEFAULT_IMAP_HOST = "imap.qq.com"
    const val DEFAULT_IMAP_PORT = 993
    const val MIN_PORT = 1
    const val MAX_PORT = 65535
    const val INVALID_PORT = -1
}
