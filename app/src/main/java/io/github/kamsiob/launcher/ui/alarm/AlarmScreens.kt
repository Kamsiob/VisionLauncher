package io.github.kamsiob.launcher.ui.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.alarm.Alarm
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.components.UndoStrip
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.serifStyle
import io.github.kamsiob.launcher.ui.theme.stepSp

/** Formats an alarm time the way the ringing screen shows it. */
@Composable
fun alarmTimeText(hour: Int, minute: Int): String =
    clockLabel(LocalContext.current, hour, minute)

/**
 * The same formatting without a composition, so screens that need to pass a
 * formatter down as a plain function can share it rather than growing a second
 * one that drifts.
 */
fun clockLabel(context: android.content.Context, hour: Int, minute: Int): String =
    if (android.text.format.DateFormat.is24HourFormat(context)) {
        "%d:%02d".format(hour, minute)
    } else {
        val display = if (hour % 12 == 0) 12 else hour % 12
        val suffix = context.getString(
            if (hour < 12) R.string.alarm_am else R.string.alarm_pm
        )
        "%d:%02d %s".format(display, minute, suffix)
    }

/** Big digits, one tap to turn an alarm on or off. */
@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    onToggle: (Alarm) -> Unit,
    onEdit: (Alarm) -> Unit,
    onNew: () -> Unit,
    onHome: () -> Unit,
    justRemoved: Alarm? = null,
    onPutItBack: () -> Unit = {},
) {
    ScreenFrame(topBar = { TopBar(onHome = onHome) }) {
        ScreenTitle(stringResource(R.string.alarms_title))
        if (alarms.isEmpty()) {
            NoteText(stringResource(R.string.alarm_none_yet))
        }
        alarms.forEach { alarm ->
            val time = alarmTimeText(alarm.hour, alarm.minute)
            val state = stringResource(if (alarm.enabled) R.string.alarm_on else R.string.alarm_off)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                RowKey(
                    label = time,
                    meta = alarm.label.ifBlank { null },
                    contentDescription = stringResource(R.string.a11y_alarm_row, time, state),
                    onClick = { onEdit(alarm) },
                    modifier = Modifier.weight(1f),
                )
                ApplianceKey(
                    label = state,
                    onClick = { onToggle(alarm) },
                    style = if (alarm.enabled) KeyStyle.PRIMARY else KeyStyle.NORMAL,
                    minHeight = Dimens.rowKey,
                    fontSize = TypeScale.keyLabelSmall,
                    committing = true,
                    contentDescription = stringResource(
                        if (alarm.enabled) R.string.a11y_alarm_toggle_off
                        else R.string.a11y_alarm_toggle_on,
                        time,
                    ),
                    modifier = Modifier.weight(0.5f),
                )
            }
        }
        // Every other removal in this app offers the lamp strip and an undo.
        // Taking an alarm off was the one that did neither, and the sentence
        // written for it had never been used.
        justRemoved?.let {
            UndoStrip(
                message = stringResource(R.string.alarm_deleted),
                actionLabel = stringResource(R.string.put_it_back),
                onAction = onPutItBack,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        ApplianceKey(
            label = stringResource(R.string.alarm_new),
            onClick = onNew,
            style = KeyStyle.PRIMARY,
        )
    }
}

/**
 * Setting a time with keys rather than a wheel, because a wheel is a drag and
 * this app requires no dragging anywhere.
 */
@Composable
fun AlarmEditScreen(
    existing: Alarm?,
    onSave: (hour: Int, minute: Int, label: String) -> Unit,
    onDelete: (() -> Unit)?,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalPalette.current
    var hour by remember { mutableIntStateOf(existing?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: 0) }
    var label by remember { mutableStateOf(existing?.label.orEmpty()) }
    val labelFieldDescription = stringResource(R.string.a11y_alarm_label_field)

    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(if (existing == null) R.string.alarm_new else R.string.alarms_title))
        // "8:00 AM" is wider than the clock's 94sp allows, so the time holds
        // the clock size and steps down only as far as it must to stay on one
        // line.
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
            value = label,
            onValueChange = { label = it },
            textStyle = bodyStyle(size = TypeScale.keyLabelSmall, weight = FontWeight.Medium),
            placeholder = {
                Text(
                    text = stringResource(R.string.alarm_label_hint),
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
                // Text fields default to 56dp, under the 72dp floor this app
                // holds every other thing you touch to. A field is a target
                // like any other.
                .heightIn(min = Dimens.keySmall)
                .semantics { contentDescription = labelFieldDescription },
        )
        Spacer(modifier = Modifier.weight(1f))
        ApplianceKey(
            label = stringResource(R.string.alarm_save),
            onClick = { onSave(hour, minute, label) },
            style = KeyStyle.PRIMARY,
            committing = true,
        )
        if (onDelete != null) {
            ApplianceKey(
                label = stringResource(R.string.alarm_delete),
                onClick = onDelete,
                committing = true,
            )
        }
    }
}
