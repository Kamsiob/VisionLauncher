package io.github.kamsiob.launcher.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.today.TodayCard
import io.github.kamsiob.launcher.ui.alarm.alarmTimeText
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
import io.github.kamsiob.launcher.ui.theme.serifStyle
import io.github.kamsiob.launcher.ui.theme.stepSp

/**
 * Adding or changing one thing on the Today screen.
 *
 * The same shape as the alarm editor on purpose: the time is set with four
 * large keys rather than a wheel, because a wheel needs a drag that a tremor
 * turns into a fling.
 */
@Composable
fun TodayEditScreen(
    existing: TodayCard?,
    onSave: (hour: Int, minute: Int, what: String) -> Unit,
    onDelete: (() -> Unit)?,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalPalette.current
    var hour by remember { mutableIntStateOf(existing?.hour ?: 9) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: 0) }
    var what by remember { mutableStateOf(existing?.what.orEmpty()) }
    val fieldDescription = stringResource(R.string.today_what_hint)

    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.today_edit_title))

        BasicText(
            text = alarmTimeText(hour, minute),
            style = serifStyle(size = TypeScale.clock, lineHeightFactor = 1.05f)
                .copy(color = palette.accent, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = stepSp(TypeScale.title),
                maxFontSize = stepSp(TypeScale.clock),
                stepSize = 2.sp,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            ApplianceKey(
                label = stringResource(R.string.alarm_minus_hour),
                onClick = { hour = (hour + 23) % 24 },
                modifier = Modifier.weight(1f),
                fontSize = TypeScale.keyLabelSmall,
                repeatable = true,
            )
            ApplianceKey(
                label = stringResource(R.string.alarm_plus_hour),
                onClick = { hour = (hour + 1) % 24 },
                modifier = Modifier.weight(1f),
                fontSize = TypeScale.keyLabelSmall,
                repeatable = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            ApplianceKey(
                label = stringResource(R.string.alarm_minus_minute),
                onClick = { minute = (minute + 55) % 60 },
                modifier = Modifier.weight(1f),
                fontSize = TypeScale.keyLabelSmall,
                repeatable = true,
            )
            ApplianceKey(
                label = stringResource(R.string.alarm_plus_minute),
                onClick = { minute = (minute + 5) % 60 },
                modifier = Modifier.weight(1f),
                fontSize = TypeScale.keyLabelSmall,
                repeatable = true,
            )
        }

        OutlinedTextField(
            value = what,
            onValueChange = { what = it },
            textStyle = bodyStyle(size = TypeScale.keyLabelSmall, weight = FontWeight.Medium),
            placeholder = {
                Text(
                    text = stringResource(R.string.today_what_hint),
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
                .semantics { contentDescription = fieldDescription },
        )

        Spacer(modifier = Modifier.weight(1f))

        ApplianceKey(
            label = stringResource(R.string.today_save),
            onClick = { onSave(hour, minute, what.trim()) },
            style = KeyStyle.PRIMARY,
            committing = true,
            // A card with no words would show an empty box nobody could act on.
            enabled = what.isNotBlank(),
        )
        if (onDelete != null) {
            ApplianceKey(
                label = stringResource(R.string.today_remove),
                onClick = onDelete,
                committing = true,
            )
        }
        NoteText(stringResource(R.string.today_not_medical))
    }
}
