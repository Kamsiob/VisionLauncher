package io.github.kamsiob.launcher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.monoStyle

/**
 * The four digit code on helper settings, entered or chosen on the same screen.
 *
 * Built from the same 96dp keypad keys as dialing, so it is a pad somebody has
 * already used rather than a new control. Deliberately not a security feature,
 * and the screen says so: it stops a setting being changed by accident.
 */
@Composable
fun PinScreen(
    setting: Boolean,
    onAccepted: (String) -> Unit,
    verify: (String) -> Boolean,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalPalette.current
    val view = LocalView.current
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    fun submit(code: String) {
        if (setting) {
            Haptics.confirm(view)
            onAccepted(code)
        } else if (verify(code)) {
            Haptics.confirm(view)
            onAccepted(code)
        } else {
            Haptics.reject(view)
            wrong = true
            entered = ""
        }
    }

    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.pin_title))
        Text(
            text = stringResource(if (setting) R.string.pin_set else R.string.pin_enter),
            style = bodyStyle(),
            color = palette.text,
        )

        // Dots rather than digits, and pinned left to right so the first digit
        // typed is always the leftmost one, in any layout direction.
        val spoken = stringResource(R.string.a11y_pin_entered, entered.length)
        Text(
            text = "●".repeat(entered.length) + "○".repeat(4 - entered.length),
            style = monoStyle(size = TypeScale.dialed)
                .copy(letterSpacing = 0.3.em, textDirection = TextDirection.Ltr),
            color = palette.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = spoken
                    liveRegion = LiveRegionMode.Polite
                },
        )

        if (wrong) {
            Text(
                text = stringResource(R.string.pin_wrong),
                style = bodyStyle(),
                color = palette.red,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }

        // Left to right for the same reason the dialer's pad is: a numeric pad
        // is a physical arrangement people know by position, and mirroring it
        // in an Arabic layout put 1 where 3 belongs.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
                .forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                        row.forEach { digit ->
                            ApplianceKey(
                                label = digit,
                                onClick = {
                                    wrong = false
                                    if (entered.length < 4) entered += digit
                                    if (entered.length == 4) submit(entered)
                                },
                                modifier = Modifier.weight(1f),
                                minHeight = Dimens.keypadKey,
                                fontSize = TypeScale.padKey,
                                repeatable = true,
                            )
                        }
                    }
                }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                ApplianceKey(
                    label = stringResource(R.string.keypad_erase),
                    onClick = { wrong = false; entered = entered.dropLast(1) },
                    modifier = Modifier.weight(1f),
                    minHeight = Dimens.keypadKey,
                    fontSize = TypeScale.keyLabelSmall,
                    repeatable = true,
                )
                ApplianceKey(
                    label = "0",
                    onClick = {
                        wrong = false
                        if (entered.length < 4) entered += "0"
                        if (entered.length == 4) submit(entered)
                    },
                    modifier = Modifier.weight(1f),
                    minHeight = Dimens.keypadKey,
                    fontSize = TypeScale.padKey,
                    repeatable = true,
                )
                ApplianceKey(
                    label = stringResource(R.string.keypad_clear),
                    onClick = { wrong = false; entered = "" },
                    modifier = Modifier.weight(1f),
                    minHeight = Dimens.keypadKey,
                    fontSize = TypeScale.keyLabelSmall,
                )
            }
        }
        }

        NoteText(stringResource(if (setting) R.string.pin_explain else R.string.pin_forgot))
    }
}

/** Grid 15's phrase list, six slots a helper can rewrite one at a time. */
@Composable
fun PhrasesScreen(
    phrases: List<String>,
    onEdit: (Int) -> Unit,
    onReset: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.phrases_title))
        NoteText(stringResource(R.string.phrases_explain))
        phrases.forEachIndexed { index, phrase ->
            ApplianceKey(
                label = phrase,
                contentDescription = stringResource(R.string.a11y_phrase_slot, index + 1, phrase),
                onClick = { onEdit(index) },
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
            )
        }
        ApplianceKey(
            label = stringResource(R.string.phrases_reset),
            onClick = onReset,
            minHeight = Dimens.keySmall,
            fontSize = TypeScale.keyLabelSmall,
            style = KeyStyle.NORMAL,
            committing = true,
        )
    }
}
