package io.github.kamsiob.launcher.messages

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification

/**
 * Sending a reply by firing the notification's own inline action.
 *
 * The app never takes the SMS role, so this is how a reply reaches the person:
 * through the app that delivered the message, on its own transport. See
 * DECISIONS D2 and D3.
 */
object Replying {

    /**
     * Returns true only when the reply was actually handed to the other app.
     *
     * Every failure here is silent by nature: the notification may have been
     * dismissed between opening the screen and pressing send, the action may
     * have been revoked, or the app may have been stopped. The caller has to be
     * told, because a reply that quietly never sends is the worst outcome this
     * screen can produce.
     */
    fun send(context: Context, notificationKey: String?, text: String): Boolean {
        if (text.isBlank()) return false
        val service = MessageListener.connected ?: return false
        val active = runCatching { service.activeNotifications }.getOrNull()
        val sbn = active?.firstOrNull { it.key == notificationKey } ?: return false
        val action = sbn.notification?.let { replyAction(it) } ?: return false
        val input = replyInput(action) ?: return false
        return runCatching {
            val intent = Intent()
            val results = Bundle().apply { putCharSequence(input.resultKey, text) }
            RemoteInput.addResultsToIntent(arrayOf(input), intent, results)
            // Some apps only accept the reply when told it was typed by a person.
            RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT)
            action.actionIntent.send(context, 0, intent)
            true
        }.getOrDefault(false)
    }

    /** The escape hatch that always exists, whatever the pipeline did. */
    fun openSourceApp(context: Context, packageName: String): Boolean = runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return@runCatching false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    /** Any messaging app at all, for when there is no particular one to open. */
    fun openAnyMessagingApp(context: Context): Boolean = runCatching {
        val intent = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
