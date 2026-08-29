package io.github.kamsiob.launcher.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.SectLabel
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.sideBySideFits

/**
 * Grid 09. The keyboard problem, answered without building a keyboard.
 *
 * Speaking is the primary path, six phrases are the zero effort path, and the
 * system keyboard is the fallback. Whatever was captured is shown large and
 * confirmed before it goes anywhere, because a reply is the one thing on this
 * screen that cannot be taken back.
 */
@Composable
fun ReplyScreen(
    senderName: String,
    phrases: List<String>,
    canDictate: Boolean,
    listening: Boolean,
    heard: String?,
    lastError: String?,
    onSpeak: () -> Unit,
    onSend: (String) -> Unit,
    onType: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalPalette.current

    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.reply_title, senderName))

        if (lastError != null) {
            Text(
                text = lastError,
                style = bodyStyle(),
                color = palette.text,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }

        if (heard != null) {
            // Shown back before it is sent. Recognition mishears, and a reply
            // sent on a guess is a reply the person never wrote.
            val spoken = stringResource(R.string.a11y_reply_confirm, heard)
            Text(
                text = "“$heard”",
                style = bodyStyle(size = TypeScale.bigmsg, lineHeightFactor = 1.35f),
                color = palette.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = spoken
                        liveRegion = LiveRegionMode.Assertive
                    },
            )
            ApplianceKey(
                label = stringResource(R.string.reply_send),
                onClick = { onSend(heard) },
                style = KeyStyle.PRIMARY,
                committing = true,
            )
            ApplianceKey(
                label = stringResource(R.string.reply_say_again),
                icon = LineIcons.microphone,
                onClick = onSpeak,
                minHeight = Dimens.keySmall,
            )
        } else if (canDictate) {
            ApplianceKey(
                label = stringResource(
                    if (listening) R.string.reply_listening else R.string.reply_speak
                ),
                icon = LineIcons.microphone,
                onClick = onSpeak,
                style = KeyStyle.PRIMARY,
                minHeight = Dimens.bigKey,
                enabled = !listening,
            )
        }

        SectLabel(stringResource(R.string.reply_or_send_one))
        PhraseGrid(phrases = phrases, onSend = onSend)

        ApplianceKey(
            label = stringResource(R.string.reply_type_instead),
            onClick = onType,
            minHeight = Dimens.keySmall,
        )

        // The privacy sentence appears only where it is true. Where the device
        // has no on-device recognizer there is no speak key above it either.
        if (canDictate) NoteText(stringResource(R.string.reply_voice_note))
    }
}

/**
 * Two columns as the grid draws them, one column once the text is large
 * enough that two would hyphenate a phrase. Same threshold as the home tiles,
 * so the whole app reflows at one point rather than screen by screen.
 */
@Composable
private fun PhraseGrid(phrases: List<String>, onSend: (String) -> Unit) {
    val twoUp = sideBySideFits()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        if (twoUp) {
            phrases.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                    pair.forEach { phrase ->
                        PhraseKey(phrase, Modifier.weight(1f), onSend)
                    }
                    // Keeps a lone phrase on the last row half width rather
                    // than stretched to twice its neighbors.
                    if (pair.size == 1) Column(Modifier.weight(1f)) {}
                }
            }
        } else {
            phrases.forEach { phrase -> PhraseKey(phrase, Modifier.fillMaxWidth(), onSend) }
        }
    }
}

@Composable
private fun PhraseKey(phrase: String, modifier: Modifier, onSend: (String) -> Unit) {
    ApplianceKey(
        label = phrase,
        onClick = { onSend(phrase) },
        modifier = modifier,
        minHeight = Dimens.phraseKey,
        fontSize = TypeScale.keyLabelSmall,
        committing = true,
    )
}
