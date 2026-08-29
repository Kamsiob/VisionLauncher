package io.github.kamsiob.launcher.ui.threshold

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.nav.SystemDestination
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.BodyText
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.SerifHeading
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale

/**
 * Grid 21. Shown before every handoff to a system screen: what the next
 * screen is, that it looks different and smaller, and that Home returns
 * them. Dismissible per destination; Helper settings restores all warnings.
 */
@Composable
fun ThresholdScreen(
    destination: SystemDestination,
    onContinue: () -> Unit,
    onStay: () -> Unit,
    onDismissForever: () -> Unit,
    onHome: () -> Unit,
) {
    val destinationName = stringResource(destination.labelRes)
    ScreenFrame(topBar = { TopBar(onHome = onHome) }) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = LineIcons.door,
            contentDescription = null,
            tint = LocalPalette.current.accent,
            modifier = Modifier.size(Dimens.thresholdIcon),
        )
        SerifHeading(stringResource(R.string.threshold_title))
        BodyText(stringResource(R.string.threshold_body, destinationName))
        BodyText(stringResource(R.string.threshold_return))
        Spacer(modifier = Modifier.weight(1f))
        ApplianceKey(
            label = stringResource(R.string.threshold_continue, destinationName),
            onClick = onContinue,
            style = KeyStyle.PRIMARY,
        )
        ApplianceKey(
            label = stringResource(R.string.threshold_stay),
            onClick = onStay,
        )
        ApplianceKey(
            label = stringResource(R.string.threshold_dont_warn, destinationName),
            onClick = onDismissForever,
            minHeight = Dimens.dismissKey,
            fontSize = 20,
            committing = true,
        )
    }
}
