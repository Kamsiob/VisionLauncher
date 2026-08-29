package io.github.kamsiob.launcher.messages

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.kamsiob.launcher.support.Speaking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the message screens read and write.
 *
 * Held on the activity rather than in a composable so that opening a message,
 * replying, and coming back does not re-query the database three times, and so
 * a reply in flight survives the recomposition that follows it.
 */
class MessagesState(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val dao = MessageDatabase.get(context).messages()

    val messages: Flow<List<StoredMessage>> = dao.recent()

    val unreadToday: Flow<Int> = dao.unreadSince(MessageTime.startOfToday())

    /** Recomputed on every visit, because access can be revoked while away. */
    fun hasAccess(): Boolean = hasNotificationAccess(context)

    /** The message currently being read or replied to. */
    var open by mutableStateOf<StoredMessage?>(null)
        private set

    var listening by mutableStateOf(false)
        private set

    var heard by mutableStateOf<String?>(null)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    var speaking by mutableStateOf(false)
        private set

    private val dictation = Dictation(context)
    private val speech = Speaking(context)

    val canDictate: Boolean get() = dictation.available
    val canSpeakAloud: Boolean get() = speech.available

    fun openMessage(message: StoredMessage) {
        open = message
        heard = null
        lastError = null
        scope.launch { dao.markRead(message.id, System.currentTimeMillis()) }
        // The notification is deliberately left alone. Dismissing it here looks
        // tidy and destroys the reply action, which lives on the notification
        // and dies with it, so reading a message would take away the ability to
        // answer it. See DECISIONS D43.
    }

    fun closeMessage() {
        stopSpeaking()
        cancelDictation()
        open = null
        heard = null
        lastError = null
    }

    fun readAloud() {
        val message = open ?: return
        val text = if (message.redacted) {
            context.getString(
                io.github.kamsiob.launcher.R.string.message_hidden_explained,
                message.appLabel,
            )
        } else {
            message.body
        }
        if (text.isBlank()) return
        speaking = true
        speech.speak("${message.sender}. $text") { speaking = false }
    }

    fun stopSpeaking() {
        speech.stop()
        speaking = false
    }

    fun startDictation() {
        heard = null
        lastError = null
        listening = true
        dictation.start(
            onResult = { text ->
                listening = false
                heard = text
            },
            onNothingHeard = {
                listening = false
                lastError = context.getString(
                    io.github.kamsiob.launcher.R.string.reply_heard_nothing
                )
            },
        )
    }

    fun cancelDictation() {
        dictation.stop()
        listening = false
    }

    /**
     * Returns true only when the reply actually went out. The caller says so
     * either way, because a reply that silently failed is the worst thing this
     * screen could do.
     */
    fun sendReply(text: String): Boolean {
        val message = open ?: return false
        cancelDictation()
        val sent = Replying.send(context, message.notificationKey, text)
        if (!sent) {
            // The action was there when the screen opened and is not there now,
            // so the stored row is stale. Correcting it stops the inbox
            // offering a reply that cannot be sent a second time.
            message.notificationKey?.let { key ->
                scope.launch { dao.forgetReplyAction(key) }
            }
            lastError = context.getString(
                io.github.kamsiob.launcher.R.string.reply_failed,
                message.appLabel,
            )
        } else {
            heard = null
            lastError = null
        }
        return sent
    }

    fun release() {
        dictation.stop()
        speech.release()
    }
}
