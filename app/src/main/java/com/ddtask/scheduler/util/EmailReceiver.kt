package com.ddtask.scheduler.util

import android.content.Context
import java.util.Properties
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.UIDFolder
import javax.mail.internet.MimeMultipart
import javax.mail.search.FlagTerm

/** 通过 IMAP 轮询发件邮箱收件箱，读取含关键字的邮件。 */
object EmailReceiver {

    data class IncomingEmail(
        val uid: Long,
        val text: String
    )

    /** 拉取尚未处理的新邮件；失败时返回空列表。 */
    fun pollNewMessages(context: Context): List<IncomingEmail> {
        val storage = NotificationStorage(context.applicationContext)
        if (!storage.isSenderMailboxReady() || !storage.emailTriggerEnabled) return emptyList()

        var folder: Folder? = null
        var store: javax.mail.Store? = null
        return try {
            val session = Session.getInstance(Properties())
            store = session.getStore("imaps")
            store.connect(
                storage.resolvedImapHost(),
                storage.senderEmail,
                storage.senderPassword
            )
            folder = store.getFolder("INBOX")
            folder.open(Folder.READ_WRITE)

            val uidFolder = folder as UIDFolder
            val lastUid = storage.lastProcessedImapUid
            val messages = if (lastUid <= 0L) {
                folder.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
            } else {
                uidFolder.getMessagesByUID(lastUid + 1, UIDFolder.MAXUID)
            }

            val result = mutableListOf<IncomingEmail>()
            var maxUid = lastUid
            for (message in messages) {
                val uid = uidFolder.getUID(message)
                if (uid <= lastUid) continue
                val text = extractMessageText(message)
                if (text.isNotBlank()) {
                    result.add(IncomingEmail(uid, text))
                }
                message.setFlag(Flags.Flag.SEEN, true)
                if (uid > maxUid) maxUid = uid
            }
            if (maxUid > lastUid) {
                storage.lastProcessedImapUid = maxUid
            }
            result
        } catch (_: Exception) {
            emptyList()
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

    private fun extractMessageText(message: Message): String {
        val parts = mutableListOf<String>()
        message.subject?.let { parts.add(it) }
        extractPart(message, parts)
        return parts.joinToString(" ").replace(Regex("\\s+"), " ").trim()
    }

    private fun extractPart(part: Part, parts: MutableList<String>) {
        try {
            when {
                part.isMimeType("text/plain") -> {
                    parts.add(part.content.toString())
                }
                part.isMimeType("text/html") -> {
                    parts.add(stripHtml(part.content.toString()))
                }
                part.isMimeType("multipart/*") -> {
                    val multipart = part.content as Multipart
                    for (i in 0 until multipart.count) {
                        extractPart(multipart.getBodyPart(i), parts)
                    }
                }
                part.content is MimeMultipart -> {
                    val multipart = part.content as MimeMultipart
                    for (i in 0 until multipart.count) {
                        extractPart(multipart.getBodyPart(i), parts)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .trim()
    }
}
