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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    // The initial value must not be the query itself. Enumerating every
    // launchable activity, resolving each label and sorting the result took a
    // measured 150ms at the 95th percentile, and as a produceState initial
    // value that ran during composition on the main thread, so opening this
    // screen dropped roughly eighteen frames. It runs on the IO dispatcher now
    // and the screen appears immediately with its list arriving a moment later.
    val allApps by produceState(initialValue = emptyList<AppsRepository.AppEntry>(), apps) {
        value = withContext(Dispatchers.IO) { apps.launchableApps() }
        apps.changes().collect {
            value = withContext(Dispatchers.IO) { apps.launchableApps() }
        }
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
                // The note has to match what actually happens. A new tile fills
                // the first empty spot, which is only the bottom when the empty
                // spot happens to be last, so the screen says which one it is.
                NoteText(
                    stringResource(
                        if (currentLayout.any { it.isEmpty }) R.string.arrange_add_app_note_gap
                        else R.string.arrange_add_app_note
                    )
                )
            }
        }
    }
}
