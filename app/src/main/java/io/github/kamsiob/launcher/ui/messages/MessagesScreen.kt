package io.github.kamsiob.launcher.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.messages.Replying
import io.github.kamsiob.launcher.messages.StoredMessage
import io.github.kamsiob.launcher.messages.MessageTime
import io.github.kamsiob.launcher.ui.call.avatarColorFor
import io.github.kamsiob.launcher.ui.call.initialOf
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.StatusPill
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens

/**
 * Grid 07. The inbox.
 *
 * Every row says who, when, which app, and what was said, because the app the
 * message arrived through is part of knowing who is talking to you. Rows come
 * from the local store rather than the notification shade, so yesterday's
 * message is still here after its notification is gone.
 */
@Composable
fun MessagesScreen(
    messages: List<StoredMessage>,
    unreadToday: Int,
    hasAccess: Boolean,
    onOpen: (StoredMessage) -> Unit,
    onGrantAccess: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    ScreenFrame(scrollable = false) {
        TopBar(onHome = onHome)
        ScreenTitle(stringResource(R.string.messages_title))

        if (!hasAccess) {
            // Without notification access there is no pipeline at all, and an
            // empty list would read as "nobody has written to you". The screen
            // has to own the difference.
            NoteText(stringResource(R.string.messages_no_access))
            ApplianceKey(
                label = stringResource(R.string.messages_grant_access),
                onClick = onGrantAccess,
                style = KeyStyle.PRIMARY,
            )
        } else if (unreadToday > 0) {
            StatusPill(
                text = pluralStringResource(
                    R.plurals.messages_new_today, unreadToday, unreadToday
                ),
                announce = true,
            )
        }

        // Only where the app can actually see messages. Without access an
        // empty list is not the same fact as nobody having written, and saying
        // both put a true sentence next to a misleading one.
        if (messages.isEmpty() && hasAccess) {
            NoteText(stringResource(R.string.messages_none_yet))
        }

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageRow(message = message, onOpen = { onOpen(message) })
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            NoteText(stringResource(R.string.messages_kept_here))
            // The reliable fallback. Whatever the pipeline missed, this key
            // still reaches the person's real messaging app.
            ApplianceKey(
                label = stringResource(R.string.open_message_app),
                onClick = { Replying.openAnyMessagingApp(context) },
                minHeight = Dimens.keySmall,
            )
        }
    }
}

@Composable
private fun MessageRow(message: StoredMessage, onOpen: () -> Unit) {
    val when_ = MessageTime.relative(LocalContext.current, message.postedAt)
    val body = when {
        message.redacted -> stringResource(R.string.messages_hidden_body)
        message.body.isNotEmpty() -> "“${message.body.lineSequence().first()}”"
        else -> stringResource(R.string.messages_sent_something)
    }
    val meta = stringResource(R.string.messages_row_meta, when_, message.appLabel, body)
    RowKey(
        label = message.sender,
        meta = meta,
        avatarInitial = initialOf(message.sender),
        avatarColor = avatarColorFor(message.sender),
        contentDescription = stringResource(R.string.a11y_message_row, message.sender, meta),
        onClick = onOpen,
    )
}
