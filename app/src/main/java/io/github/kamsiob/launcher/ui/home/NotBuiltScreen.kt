package io.github.kamsiob.launcher.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.BodyText
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar

/**
 * The honest interstitial behind tiles whose feature belongs to a later
 * stage. It says so plainly and, where the phone has its own way to do the
 * job, offers that instead. This screen deletes itself stage by stage.
 */
@Composable
fun NotBuiltScreen(
    feature: BuiltIn,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    ScreenFrame(topBar = { TopBar(onHome = onHome) }) {
        ScreenTitle(feature.label())
        BodyText(stringResource(R.string.not_built_body))
        Spacer(modifier = Modifier.weight(1f))
        when (feature) {
            BuiltIn.MESSAGES -> ApplianceKey(
                label = stringResource(R.string.open_message_app),
                onClick = {
                    val intent = Intent.makeMainSelectorActivity(
                        Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
            )
            BuiltIn.PHOTOS -> ApplianceKey(
                label = stringResource(R.string.open_photos_app),
                onClick = {
                    val intent = Intent.makeMainSelectorActivity(
                        Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
            )
            else -> {}
        }
    }
}
