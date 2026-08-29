package io.github.kamsiob.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.SectLabel
import io.github.kamsiob.launcher.ui.components.StatusPill
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.components.keySurface
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.support.debouncedClickable
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.Look
import io.github.kamsiob.launcher.ui.theme.TextStep
import io.github.kamsiob.launcher.ui.theme.Tokens
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.tileColumns
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.monoStyle
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

/**
 * Grid 14. The simple tier on one screen: text size, Look, Choose your apps,
 * Put my screen back, See and hear better, Helper settings. One note, which
 * qualifies as a brand promise.
 */
@Composable
fun SettingsScreen(
    look: Look,
    outlined: Boolean,
    textStep: TextStep,
    onSetLook: (Look) -> Unit,
    onSetOutlined: (Boolean) -> Unit,
    onSetTextStep: (TextStep) -> Unit,
    onChooseApps: () -> Unit,
    onRestore: () -> Unit,
    onSeeHear: () -> Unit,
    onHelper: () -> Unit,
    onHome: () -> Unit,
) {
    val view = LocalView.current
    var restored by remember { mutableStateOf(false) }
    LaunchedEffect(restored) {
        if (restored) {
            delay(6000)
            restored = false
        }
    }

    ScreenFrame(topBar = { TopBar(onHome = onHome) }) {
        ScreenTitle(stringResource(R.string.settings_title))

        SectLabel(stringResource(R.string.settings_text_size))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            TextStep.entries.forEachIndexed { index, step ->
                SizeKey(
                    step = step,
                    index = index,
                    selected = step == textStep,
                    onClick = {
                        Haptics.confirm(view)
                        onSetTextStep(step)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectLabel(stringResource(R.string.settings_look))
        ThemeChoices(
            look = look,
            outlined = outlined,
            onSetLook = {
                Haptics.confirm(view)
                onSetLook(it)
            },
            onToggleOutlined = {
                Haptics.confirm(view)
                onSetOutlined(!outlined)
            },
        )

        RowKey(
            label = stringResource(R.string.settings_choose_apps),
            meta = stringResource(R.string.settings_choose_apps_sub),
            icon = LineIcons.grid,
            onClick = onChooseApps,
        )
        RowKey(
            label = stringResource(R.string.settings_restore),
            meta = stringResource(R.string.settings_restore_sub),
            icon = LineIcons.restore,
            committing = true,
            onClick = {
                onRestore()
                restored = true
            },
        )
        if (restored) {
            StatusPill(text = stringResource(R.string.settings_restore_done))
        }
        RowKey(
            label = stringResource(R.string.settings_see_hear),
            meta = stringResource(R.string.settings_see_hear_sub),
            icon = LineIcons.accessibilitySun,
            onClick = onSeeHear,
        )
        RowKey(
            label = stringResource(R.string.settings_helper),
            meta = stringResource(R.string.settings_helper_sub),
            icon = LineIcons.lock,
            onClick = onHelper,
        )
        NoteText(stringResource(R.string.settings_undo_note))
    }
}

@Composable
private fun ThemeChoices(
    look: Look,
    outlined: Boolean,
    onSetLook: (Look) -> Unit,
    onToggleOutlined: () -> Unit,
) {
    val cards: List<@Composable (Modifier) -> Unit> = listOf(
        { m ->
            ThemeCard(
                label = stringResource(R.string.settings_look_light),
                selected = look == Look.LIGHT,
                preview = { MiniPreview(background = Tokens.paper, border = null) },
                onClick = { onSetLook(Look.LIGHT) },
                modifier = m,
            )
        },
        { m ->
            ThemeCard(
                label = stringResource(R.string.settings_look_dark),
                selected = look == Look.DARK,
                preview = { MiniPreview(background = Tokens.darkBg, border = null) },
                onClick = { onSetLook(Look.DARK) },
                modifier = m,
            )
        },
        { m ->
            ThemeCard(
                label = stringResource(R.string.settings_look_outlined),
                selected = outlined,
                preview = { MiniPreview(background = Tokens.paper, border = Tokens.ink) },
                onClick = onToggleOutlined,
                modifier = m,
            )
        },
    )
    if (tileColumns() == 1) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            cards.forEach { it(Modifier.fillMaxWidth()) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            cards.forEach { card ->
                Box(modifier = Modifier.weight(1f)) { card(Modifier.fillMaxWidth()) }
            }
        }
    }
}

/** The navy selection ring: 4dp, sitting just outside the key. */
@Composable
fun SelectionRing(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.border(
                        Dimens.liftedRing,
                        palette.accent,
                        RoundedCornerShape(Dimens.radiusKey + Dimens.liftedRingOffset),
                    )
                } else {
                    Modifier
                }
            )
            .padding(Dimens.liftedRingOffset + 2.dp),
    ) {
        content()
    }
}

@Composable
private fun SizeKey(
    step: TextStep,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val letterSize = when (step) {
        TextStep.ONE -> 24
        TextStep.TWO -> 32
        TextStep.THREE -> 42
    }
    val description = stringResource(R.string.a11y_text_size_step, index + 1)
    SelectionRing(selected = selected, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .keySurface()
                .debouncedClickable { onClick() }
                .padding(vertical = 12.dp)
                .clearAndSetSemantics {
                    contentDescription = description
                    this.selected = selected
                    role = Role.RadioButton
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "A",
                style = bodyStyle(size = letterSize, weight = FontWeight.Bold, lineHeightFactor = 1.1f),
                color = palette.text,
            )
            if (selected) {
                Text(
                    text = stringResource(R.string.settings_size_now),
                    style = monoStyle(size = TypeScale.nowTag, weight = FontWeight.Bold),
                    color = palette.accent,
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    label: String,
    selected: Boolean,
    preview: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val cardDescription = if (selected) {
        stringResource(R.string.a11y_look_selected, label)
    } else {
        label
    }
    SelectionRing(selected = selected, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .keySurface()
                .debouncedClickable { onClick() }
                .padding(horizontal = 6.dp, vertical = 12.dp)
                .clearAndSetSemantics {
                    contentDescription = cardDescription
                    this.selected = selected
                    role = Role.RadioButton
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            preview()
            Text(
                text = label,
                style = bodyStyle(size = TypeScale.sect, weight = FontWeight.Bold, lineHeightFactor = 1.1f),
                color = palette.text,
                textAlign = TextAlign.Center,
            )
            if (selected) {
                Text(
                    text = stringResource(R.string.settings_size_now),
                    style = monoStyle(size = TypeScale.nowTag, weight = FontWeight.Bold),
                    color = palette.accent,
                )
            }
        }
    }
}

@Composable
private fun MiniPreview(background: androidx.compose.ui.graphics.Color, border: androidx.compose.ui.graphics.Color?) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(background, RoundedCornerShape(8.dp))
            .border(
                width = if (border != null) 3.dp else 1.dp,
                color = border ?: palette.hairline,
                shape = RoundedCornerShape(8.dp),
            ),
    )
}
