package io.github.kamsiob.launcher.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.messages.MessageTime
import io.github.kamsiob.launcher.messages.Replying
import io.github.kamsiob.launcher.messages.StoredMessage
import io.github.kamsiob.launcher.ui.call.avatarColorFor
import io.github.kamsiob.launcher.ui.call.initialOf
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.Avatar
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle

/**
 * Grid 08. One message, large, the same shape whatever app it came from.
 *
 * The screen has three jobs and none of them is optional: say who and when in
 * words, show the message itself at a size somebody can read across a room,
 * and be honest when there is nothing to show because the platform hid it.
 */
@Composable
fun ReadMessageScreen(
    message: StoredMessage,
    speaking: Boolean,
    canSpeak: Boolean,
    onReply: () -> Unit,
    onReadAloud: () -> Unit,
    onStopReading: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val from = stringResource(
        R.string.message_from,
        message.sender,
        message.appLabel,
        MessageTime.full(context, message.postedAt),
    )

    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(
                initial = initialOf(message.sender),
                color = avatarColorFor(message.sender),
            )
            Text(
                text = from,
                style = bodyStyle(
                    size = TypeScale.from,
                    weight = FontWeight.Bold,
                    lineHeightFactor = 1.3f,
                ),
                color = palette.text,
            )
        }

        if (message.redacted) {
            // Android 15 and later can withhold the content from any listener.
            // Saying so, and naming the app that still has it, is the whole
            // difference between a hidden message and a broken app.
            Text(
                text = stringResource(R.string.message_hidden_explained, message.appLabel),
                style = bodyStyle(),
                color = palette.text,
            )
        } else {
            Text(
                text = message.body.ifEmpty { stringResource(R.string.messages_sent_something) },
                // bodyStyle sets direction from the content, not the layout, so
                // an Arabic message inside an English launcher still reads
                // right to left.
                style = bodyStyle(
                    size = TypeScale.bigmsg,
                    weight = FontWeight.Medium,
                    lineHeightFactor = 1.4f,
                ),
                color = palette.text,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = message.body
                },
            )
        }

        if (message.canReply) {
            ApplianceKey(
                label = stringResource(R.string.message_reply),
                icon = LineIcons.reply,
                onClick = onReply,
                style = KeyStyle.PRIMARY,
                minHeight = Dimens.keySmall,
            )
        }

        if (canSpeak) {
            ApplianceKey(
                label = stringResource(
                    if (speaking) R.string.message_stop_reading else R.string.message_read_aloud
                ),
                icon = LineIcons.speaker,
                onClick = if (speaking) onStopReading else onReadAloud,
                minHeight = Dimens.keySmall,
            )
        }

        ApplianceKey(
            label = stringResource(R.string.message_open_app, message.appLabel),
            onClick = { Replying.openSourceApp(context, message.packageName) },
            minHeight = Dimens.keySmall,
        )

        NoteText(
            if (message.canReply) {
                stringResource(R.string.replies_travel, message.appLabel)
            } else {
                stringResource(R.string.message_cannot_reply)
            }
        )
    }
}
