package io.github.kamsiob.launcher.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle

/** One reply phrase, rewritten by a helper. */
@Composable
fun PhraseEditScreen(
    current: String,
    onSave: (String) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalPalette.current
    var text by remember { mutableStateOf(current) }
    val hint = stringResource(R.string.phrases_hint)

    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.phrases_edit_title))
        OutlinedTextField(
            value = text,
            onValueChange = {
                // A phrase longer than this stops fitting on its key at the
                // largest text size, and a key whose label is cut off is worse
                // than a shorter phrase.
                if (it.length <= 40) text = it
            },
            textStyle = bodyStyle(size = TypeScale.keyLabelSmall, weight = FontWeight.Medium),
            placeholder = {
                Text(
                    text = hint,
                    style = bodyStyle(size = TypeScale.keyLabelSmall, weight = FontWeight.Medium),
                    color = palette.textSoft,
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.hairline,
                cursorColor = palette.accent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = hint },
        )
        Spacer(modifier = Modifier.weight(1f))
        ApplianceKey(
            label = stringResource(R.string.phrases_save),
            onClick = { onSave(text.trim()) },
            style = KeyStyle.PRIMARY,
            committing = true,
            enabled = text.isNotBlank(),
        )
    }
}
