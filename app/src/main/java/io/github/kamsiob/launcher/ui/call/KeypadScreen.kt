package io.github.kamsiob.launcher.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.monoStyle

/** Grid 06. 96dp keys, Erase and Clear as words, the number large above. */
@Composable
fun KeypadScreen(
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var dialed by remember { mutableStateOf("") }
    val palette = LocalPalette.current

    val spokenNumber = if (dialed.isEmpty()) {
        stringResource(R.string.a11y_dialed_empty)
    } else {
        stringResource(R.string.a11y_dialed_number, dialed.toCharArray().joinToString(" "))
    }

    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.keypad_title))
        Text(
            text = formatDialed(dialed),
            // Pinned left to right. Digits carry no strong direction, so in an
            // Arabic layout the paragraph resolves right to left and reorders
            // the groups: 555 867 5309 would read 5309 867 555. A phone number
            // is the one thing on this screen that must never be reordered.
            style = monoStyle(size = TypeScale.dialed, lineHeightFactor = 1.2f)
                .copy(letterSpacing = 0.06.em, textDirection = TextDirection.Ltr),
            color = palette.text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .semantics {
                    contentDescription = spokenNumber
                    liveRegion = LiveRegionMode.Polite
                },
        )
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )
        // One grid with one gap in both directions, as the grid draws it. The
        // rows were direct children of the screen column, so they inherited its
        // 16dp and the pad had unequal gutters.
        //
        // Pinned left to right. A telephone keypad is a physical arrangement,
        // not a line of text: 1 is at the top left on every phone ever made,
        // and Android's own dialer does not mirror it either. In an Arabic
        // layout the rows flipped to 3 2 1, which puts a lifetime of muscle
        // memory in the wrong place for the people least able to absorb that.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                    row.forEach { digit ->
                        ApplianceKey(
                            label = digit,
                            onClick = { dialed += digit },
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
                    onClick = { dialed = dialed.dropLast(1) },
                    modifier = Modifier.weight(1f),
                    minHeight = Dimens.keypadKey,
                    fontSize = TypeScale.keyLabelSmall,
                    repeatable = true,
                )
                ApplianceKey(
                    label = "0",
                    onClick = { dialed += "0" },
                    modifier = Modifier.weight(1f),
                    minHeight = Dimens.keypadKey,
                    fontSize = TypeScale.padKey,
                    repeatable = true,
                )
                ApplianceKey(
                    label = stringResource(R.string.keypad_clear),
                    onClick = { dialed = "" },
                    modifier = Modifier.weight(1f),
                    minHeight = Dimens.keypadKey,
                    fontSize = TypeScale.keyLabelSmall,
                )
            }
        }
        }
        ApplianceKey(
            label = stringResource(R.string.keypad_call_this_number),
            onClick = { if (dialed.isNotEmpty()) placeCall(context, dialed) },
            style = KeyStyle.PRIMARY,
            minHeight = Dimens.keypadKey,
            committing = true,
            enabled = dialed.isNotEmpty(),
        )
    }
}

/** Groups digits in threes for the eye without touching the dialed value. */
private fun formatDialed(dialed: String): String =
    dialed.chunked(3).joinToString(" ")
