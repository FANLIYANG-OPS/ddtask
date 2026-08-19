package com.ddtask.scheduler.util

/** JavaMail / IMAP 协议配置键与常用值。 */
object MailConstants {
    const val STORE_PROTOCOL_IMAPS = "imaps"
    const val IMAP_FOLDER_INBOX = "INBOX"
    const val IMAP_CONNECTION_TIMEOUT_MS = 15_000
    const val IMAP_READ_TIMEOUT_MS = 15_000

    const val PROP_STORE_PROTOCOL = "mail.store.protocol"
    const val PROP_IMAPS_HOST = "mail.imaps.host"
    const val PROP_IMAPS_PORT = "mail.imaps.port"
    const val PROP_IMAPS_SSL_ENABLE = "mail.imaps.ssl.enable"
    const val PROP_IMAPS_CONNECTION_TIMEOUT = "mail.imaps.connectiontimeout"
    const val PROP_IMAPS_TIMEOUT = "mail.imaps.timeout"

    const val PROP_SMTP_HOST = "mail.smtp.host"
    const val PROP_SMTP_PORT = "mail.smtp.port"
    const val PROP_SMTP_AUTH = "mail.smtp.auth"
    const val PROP_SMTP_SOCKET_FACTORY_PORT = "mail.smtp.socketFactory.port"
    const val PROP_SMTP_SOCKET_FACTORY_CLASS = "mail.smtp.socketFactory.class"
    const val PROP_SMTP_SOCKET_FACTORY_FALLBACK = "mail.smtp.socketFactory.fallback"
    const val PROP_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable"

    const val PROP_VALUE_TRUE = "true"
    const val PROP_VALUE_FALSE = "false"
    const val SSL_SOCKET_FACTORY_CLASS = "javax.net.ssl.SSLSocketFactory"
    const val CHARSET_UTF8 = "UTF-8"

    const val IMAP_ID_KEY_NAME = "name"
    const val IMAP_ID_KEY_VERSION = "version"
    const val IMAP_ID_KEY_VENDOR = "vendor"
    const val IMAP_ID_KEY_SUPPORT_EMAIL = "support-email"
    const val IMAP_ID_APP_NAME = "DDTask"
    const val IMAP_ID_VENDOR = "com.ddtask.scheduler"
}
