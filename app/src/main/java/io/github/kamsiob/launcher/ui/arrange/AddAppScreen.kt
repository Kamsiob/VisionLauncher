package io.github.kamsiob.launcher.ui.arrange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.AppsRepository
import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.data.SavedTile
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.home.icon
import io.github.kamsiob.launcher.ui.home.label
import io.github.kamsiob.launcher.ui.theme.Dimens

/**
 * Grid 20. One flat alphabetical list of everything that can take a tile:
 * the built in features not already placed, then every installed app.
 */
@Composable
fun AddAppScreen(
    currentLayout: List<SavedTile>,
    apps: AppsRepository,
    onAdd: (SavedTile) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val density = LocalDensity.current
    val iconPx = with(density) { Dimens.appIcon.roundToPx() }
    val placedFeatures = currentLayout.mapNotNull { BuiltIn.fromId(it.builtIn) }.toSet()
    val availableFeatures = BuiltIn.entries.filter { it !in placedFeatures }
    val placedApps = currentLayout.mapNotNull { it.packageName }.toSet()
    val allApps by produceState(initialValue = apps.launchableApps(), apps) {
        apps.changes().collect { value = apps.launchableApps() }
    }
    val availableApps = allApps.filter { it.packageName !in placedApps }

    ScreenFrame(scrollable = false) {
        TopBar(onHome = onHome, onBack = onBack)
        ScreenTitle(stringResource(R.string.arrange_add_app))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        ) {
            items(availableFeatures, key = { "builtin_" + it.id }) { feature ->
                RowKey(
                    label = feature.label(),
                    icon = feature.icon(),
                    committing = true,
                    onClick = { onAdd(SavedTile.of(feature)) },
                )
            }
            items(availableApps, key = { it.key }) { entry ->
                RowKey(
                    label = entry.label,
                    appIcon = apps.icon(entry, iconPx),
                    committing = true,
                    onClick = { onAdd(SavedTile.ofApp(entry.packageName, entry.activity)) },
                )
            }
            item {
                NoteText(stringResource(R.string.arrange_add_app_note))
            }
        }
    }
}
