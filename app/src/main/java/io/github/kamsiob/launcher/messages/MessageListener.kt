package io.github.kamsiob.launcher.messages

import android.app.Notification
import android.app.Person
import android.app.RemoteInput
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The messaging pipeline, and the most fragile part of the app.
 *
 * Everything the listener sees is written to the local database, so the inbox
 * has real scrollback and a dismissed notification does not take the message
 * with it. Nothing is ever transmitted; the app holds no INTERNET permission
 * and a build gate enforces that.
 *
 * Built defensively on purpose. Notifications are not a documented message
 * format: every app shapes them differently, the platform redacts some of them
 * outright, and this service can be killed at any time. Every path here has to
 * degrade in a way somebody can see, because the failure this design cannot
 * afford is a message that quietly never arrives.
 */
class MessageListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val message = parse(this, sbn) ?: return
        scope.launch { MessageDatabase.get(applicationContext).messages().put(message) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // The message stays. Only the ability to reply through this particular
        // notification goes away with it, and the row must stop claiming it.
        scope.launch {
            MessageDatabase.get(applicationContext).messages().forgetReplyAction(sbn.key)
        }
    }

    override fun onListenerConnected() {
        connected = this
        // A reconnect after a kill or a reboot arrives with the whole shade, so
        // anything posted while the app was not listening is picked up here.
        scope.launch {
            val dao = MessageDatabase.get(applicationContext).messages()
            runCatching { activeNotifications }.getOrNull()?.forEach { sbn ->
                parse(this@MessageListener, sbn)?.let { dao.put(it) }
            }
            dao.trim(System.currentTimeMillis() - KEEP_FOR_MS)
        }
    }

    override fun onListenerDisconnected() {
        connected = null
    }

    override fun onDestroy() {
        connected = null
        super.onDestroy()
    }

    companion object {
        /** Six months of scrollback, then the oldest are let go. */
        private const val KEEP_FOR_MS = 180L * 24 * 60 * 60 * 1000

        /**
         * The live service, when it is bound. Reply needs the real notification
         * object, not a copy of it, because firing a RemoteInput means sending
         * the other app's own PendingIntent. Null whenever access has not been
         * granted or the platform has not bound us yet, which is exactly when
         * the reply screen must say a reply cannot be sent instead of pretending.
         */
        @Volatile
        var connected: MessageListener? = null
            private set
    }
}

/**
 * Turns a notification into a message, or returns null when it is not one.
 *
 * The filter is deliberately narrow. A launcher that hoovered up every
 * notification would fill its inbox with battery warnings and delivery updates,
 * and the screen promises messages from people.
 */
fun parse(context: Context, sbn: StatusBarNotification): StoredMessage? {
    val notification = sbn.notification ?: return null
    if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return null
    if (sbn.packageName == context.packageName) return null

    val category = notification.category
    val extras = notification.extras
    val isMessage = category == Notification.CATEGORY_MESSAGE ||
        category == Notification.CATEGORY_EMAIL ||
        extras.containsKey(Notification.EXTRA_MESSAGES) ||
        extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MessagingStyle") == true
    if (!isMessage) return null

    // A summary row for a bundle repeats what its children already said.
    if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

    // MessagingStyle first, because that is what real messaging apps use and
    // its compatibility fields lie in the ways that matter here. In a group
    // chat EXTRA_TITLE is the conversation name rather than the person who
    // wrote, and EXTRA_TEXT can carry the whole thread as one run of text.
    // Reading the style's own last message gets the right person and the right
    // sentence; the flat fields stay as the fallback for everything else.
    val latest = latestStyledMessage(notification)
    val sender = (latest?.first
        ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
        ?.trim().orEmpty()
    val body = (latest?.second
        ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
        ?.trim().orEmpty()
    val conversation = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
        ?.toString()?.trim()

    // On Android 15 and later the platform can hide sensitive content from
    // listeners entirely, and a launcher has no exemption. That is not the same
    // as an empty message, and the difference is what the row has to say.
    val redacted = Build.VERSION.SDK_INT >= 35 &&
        notification.visibility == Notification.VISIBILITY_PRIVATE &&
        body.isEmpty()

    if (sender.isEmpty() && body.isEmpty() && !redacted) return null

    return StoredMessage(
        // Keyed so the same conversation updating in place replaces its row
        // rather than stacking duplicates.
        id = sbn.key,
        // In a group chat, who wrote and where they wrote are different facts
        // and the reader needs both.
        sender = when {
            sender.isEmpty() -> appLabel(context, sbn.packageName)
            conversation.isNullOrEmpty() || conversation == sender -> sender
            else -> "$sender, in $conversation"
        },
        body = body,
        appLabel = appLabel(context, sbn.packageName),
        packageName = sbn.packageName,
        postedAt = sbn.postTime,
        redacted = redacted,
        notificationKey = sbn.key,
        canReply = replyAction(notification) != null,
    )
}

/**
 * The newest message inside a MessagingStyle notification, as sender to text.
 *
 * The platform hands these over as an array of Bundles rather than as objects,
 * and the array is not guaranteed to be sorted, so the newest is found by
 * timestamp rather than by position. Returns null for any notification that is
 * not built this way, and for a styled one whose messages are all empty, so
 * the caller can fall back rather than store a blank row.
 */
private fun latestStyledMessage(notification: Notification): Pair<String, String>? {
    // The typed getParcelableArray overload is API 33 and later, and this app
    // runs from 29. The untyped one is deprecated rather than gone, and it is
    // the only version that exists on the oldest phones supported here.
    @Suppress("DEPRECATION")
    val raw = notification.extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        ?: return null
    var bestAt = Long.MIN_VALUE
    var best: Pair<String, String>? = null
    for (parcelable in raw) {
        val item = parcelable as? Bundle ?: continue
        val text = item.getCharSequence("text")?.toString()?.trim().orEmpty()
        if (text.isEmpty()) continue
        val at = item.getLong("time", 0L)
        if (at < bestAt) continue
        // "sender" is the display name; "sender_person" is the richer object
        // newer apps send. A message the person sent themselves has neither,
        // which is how an outgoing message is told from an incoming one.
        @Suppress("DEPRECATION")
        val person = item.getParcelable("sender_person") as? Person
        val who = item.getCharSequence("sender")?.toString()?.trim()
            ?: person?.name?.toString()?.trim()
        bestAt = at
        best = (who.orEmpty()) to text
    }
    return best
}

/** The app's own name, in words, because every row says where a message came from. */
fun appLabel(context: Context, packageName: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)

/**
 * The notification's own inline reply, which is how a reply keeps its native
 * transport: RCS stays RCS and WhatsApp stays WhatsApp. See DECISIONS D2 and D3.
 */
fun replyAction(notification: Notification): Notification.Action? =
    notification.actions?.firstOrNull { action ->
        action.remoteInputs?.any { it.allowFreeFormInput } == true
    }

/** The input the reply action expects its text under. */
fun replyInput(action: Notification.Action): RemoteInput? =
    action.remoteInputs?.firstOrNull { it.allowFreeFormInput }

/** Whether the person has granted notification access, which the lamp watches. */
fun hasNotificationAccess(context: Context): Boolean {
    val enabled = android.provider.Settings.Secure.getString(
        context.contentResolver, "enabled_notification_listeners"
    ).orEmpty()
    return enabled.split(":").any { it.contains(context.packageName) }
}
