package com.ddtask.scheduler.util

import android.content.Context
import android.util.Log
import com.ddtask.scheduler.BuildConfig
import com.sun.mail.imap.IMAPStore
import java.util.Properties
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.UIDFolder
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeUtility

/** 通过 IMAP 轮询发件邮箱收件箱，读取 DDTask 约定邮件。 */
object EmailReceiver {

    private const val TAG = "EmailReceiver"

    /** 标题含此字样视为 DDTask 操作邮件，仅处理并展示此类邮件正文。 */
    const val DDTASK_SUBJECT_MARKER = "考勤"

    data class IncomingEmail(
        val uid: Long,
        val subject: String,
        val body: String
    )

    data class PollResult(
        val emails: List<IncomingEmail>,
        val error: String? = null
    )

    fun isDdtaskSubject(subject: String): Boolean {
        return decodeText(subject).contains(DDTASK_SUBJECT_MARKER)
    }

    /** 拉取尚未处理的新邮件；失败时 [PollResult.error] 携带原因。 */
    fun pollNewMessages(context: Context): PollResult {
        val storage = NotificationStorage(context.applicationContext)
        if (!storage.isSenderMailboxReady() || !storage.emailTriggerEnabled) {
            return PollResult(emptyList())
        }

        var folder: Folder? = null
        var store: javax.mail.Store? = null
        return try {
            val imapHost = storage.resolvedImapHost()
            val imapPort = storage.resolvedImapPort()
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", imapHost)
                put("mail.imaps.port", imapPort.toString())
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.connectiontimeout", "15000")
                put("mail.imaps.timeout", "15000")
            }
            val session = Session.getInstance(props)
            store = session.getStore("imaps")
            store.connect(imapHost, imapPort, storage.senderEmail, storage.senderPassword)
            sendImapIdIfRequired(store, imapHost, storage.senderEmail)

            folder = store.getFolder("INBOX")
            folder.open(Folder.READ_WRITE)

            val uidFolder = folder as UIDFolder
            val lastUid = storage.lastProcessedImapUid
            if (lastUid <= 0L) {
                initializeUidCursor(storage, folder, uidFolder)
                return PollResult(emptyList())
            }

            val messages = uidFolder.getMessagesByUID(lastUid + 1, UIDFolder.MAXUID)
            val result = mutableListOf<IncomingEmail>()
            var maxUid = lastUid
            for (message in messages) {
                val uid = uidFolder.getUID(message)
                if (uid <= lastUid) continue

                val subject = decodeText(message.subject)
                val body = extractBody(message)
                message.setFlag(Flags.Flag.SEEN, true)
                if (uid > maxUid) maxUid = uid

                if (!isDdtaskSubject(subject)) continue
                if (body.isBlank()) continue
                result.add(IncomingEmail(uid, subject, body))
            }
            if (maxUid > lastUid) {
                storage.lastProcessedImapUid = maxUid
            }
            PollResult(result)
        } catch (e: Exception) {
            Log.w(TAG, "IMAP poll failed", e)
            PollResult(emptyList(), e.message ?: e.javaClass.simpleName)
        } finally {
            try {
                folder?.close(false)
            } catch (_: Exception) {
            }
            try {
                store?.close()
            } catch (_: Exception) {
            }
        }
    }

    /** 网易邮箱要求客户端在 connect 后发送 IMAP ID，否则返回 Unsafe Login。 */
    private fun sendImapIdIfRequired(store: javax.mail.Store, imapHost: String, supportEmail: String) {
        if (!ImapHelper.requiresImapId(imapHost)) return
        if (store !is IMAPStore) return
        val idParams = hashMapOf(
            "name" to "DDTask",
            "version" to BuildConfig.VERSION_NAME,
            "vendor" to "com.ddtask.scheduler",
            "support-email" to supportEmail
        )
        store.id(idParams)
    }

    /** 首次连接时跳过已有邮件，只处理此后到达的新邮件。 */
    private fun initializeUidCursor(
        storage: NotificationStorage,
        folder: Folder,
        uidFolder: UIDFolder
    ) {
        if (folder.messageCount <= 0) return
        val lastMessage = folder.getMessage(folder.messageCount)
        storage.lastProcessedImapUid = uidFolder.getUID(lastMessage)
    }

    private fun decodeText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            MimeUtility.decodeText(raw).trim()
        } catch (_: Exception) {
            raw.trim()
        }
    }

    private fun extractBody(message: Message): String = extractBodyPart(message)

    private fun extractBodyPart(part: Part): String {
        return try {
            when {
                part.isMimeType("text/plain") -> part.content.toString().trim()
                part.isMimeType("text/html") -> stripHtml(part.content.toString())
                part.isMimeType("multipart/alternative") -> extractFromMultipart(part.content as Multipart, preferPlain = true)
                part.isMimeType("multipart/*") -> extractFromMultipart(part.content as Multipart, preferPlain = true)
                part.content is MimeMultipart -> extractFromMultipart(part.content as MimeMultipart, preferPlain = true)
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractFromMultipart(multipart: Multipart, preferPlain: Boolean): String {
        var plain: String? = null
        var html: String? = null
        for (i in 0 until multipart.count) {
            val bodyPart = multipart.getBodyPart(i)
            when {
                bodyPart.isMimeType("text/plain") -> plain = bodyPart.content.toString().trim()
                bodyPart.isMimeType("text/html") -> html = stripHtml(bodyPart.content.toString())
                bodyPart.isMimeType("multipart/*") -> {
                    val nested = extractBodyPart(bodyPart)
                    if (nested.isNotBlank()) return nested
                }
            }
        }
        return if (preferPlain) {
            plain?.takeIf { it.isNotBlank() } ?: html.orEmpty()
        } else {
            html?.takeIf { it.isNotBlank() } ?: plain.orEmpty()
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
