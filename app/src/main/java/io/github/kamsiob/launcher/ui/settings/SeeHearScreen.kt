package io.github.kamsiob.launcher.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.nav.SystemDestination
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.LineIcons

/**
 * Grid 15. The guided tour of Android's own accessibility stack that almost
 * nobody finds. Every row is a threshold handoff, never a claim that this app
 * does the thing itself.
 */
@Composable
fun SeeHearScreen(
    onHandoff: (SystemDestination) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.settings_see_hear))
        RowKey(
            label = stringResource(R.string.seehear_bigger),
            meta = stringResource(R.string.seehear_bigger_sub),
            icon = LineIcons.displaySize,
            onClick = { onHandoff(SystemDestination.DISPLAY_SIZE) },
        )
        RowKey(
            label = stringResource(R.string.seehear_magnify),
            meta = stringResource(R.string.seehear_magnify_sub),
            icon = LineIcons.magnifyPlus,
            onClick = { onHandoff(SystemDestination.ACCESSIBILITY) },
        )
        RowKey(
            label = stringResource(R.string.seehear_hearing),
            meta = stringResource(R.string.seehear_hearing_sub),
            icon = LineIcons.ear,
            onClick = { onHandoff(SystemDestination.HEARING) },
        )
        RowKey(
            label = stringResource(R.string.seehear_captions),
            meta = stringResource(R.string.seehear_captions_sub),
            icon = LineIcons.captions,
            onClick = { onHandoff(SystemDestination.CAPTIONS) },
        )
        RowKey(
            label = stringResource(R.string.seehear_sound),
            meta = stringResource(R.string.seehear_sound_sub),
            icon = LineIcons.speaker,
            onClick = { onHandoff(SystemDestination.SOUND_AMPLIFIER) },
        )
        RowKey(
            label = stringResource(R.string.seehear_medical),
            meta = stringResource(R.string.seehear_medical_sub),
            icon = LineIcons.shield,
            onClick = { onHandoff(SystemDestination.MEDICAL_INFO) },
        )
    }
}
