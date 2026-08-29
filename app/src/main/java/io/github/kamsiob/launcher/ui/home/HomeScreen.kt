package io.github.kamsiob.launcher.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.attention.AttentionState
import io.github.kamsiob.launcher.attention.AttentionWatcher
import io.github.kamsiob.launcher.data.AppsRepository
import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.data.SavedTile
import io.github.kamsiob.launcher.home.DayPart
import io.github.kamsiob.launcher.nav.SystemDestination
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.AttentionLamp
import io.github.kamsiob.launcher.ui.components.LampItem
import io.github.kamsiob.launcher.ui.components.Masthead
import io.github.kamsiob.launcher.ui.components.StatusPill
import io.github.kamsiob.launcher.ui.components.Tile
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.tileColumns
import kotlinx.coroutines.delay
import java.util.Calendar

/** The label a built in feature shows on its tile. */
@Composable
fun BuiltIn.label(): String = stringResource(
    when (this) {
        BuiltIn.CALL -> R.string.feature_call
        BuiltIn.MESSAGES -> R.string.feature_messages
        BuiltIn.MAGNIFIER -> R.string.feature_magnifier
        BuiltIn.CAMERA -> R.string.feature_camera
        BuiltIn.PHOTOS -> R.string.feature_photos
        BuiltIn.ALARMS -> R.string.feature_alarms
    }
)

fun BuiltIn.icon() = when (this) {
    BuiltIn.CALL -> LineIcons.call
    BuiltIn.MESSAGES -> LineIcons.messages
    BuiltIn.MAGNIFIER -> LineIcons.magnifier
    BuiltIn.CAMERA -> LineIcons.camera
    BuiltIn.PHOTOS -> LineIcons.photos
    BuiltIn.ALARMS -> LineIcons.alarms
}

/**
 * Grid 01, 02, 03, 24. The home screen: masthead, status pill or attention
 * lamp, the fixed tiles, and More apps. Nothing here ever reorders itself.
 */
@Composable
fun HomeScreen(
    layout: List<SavedTile>,
    apps: AppsRepository,
    watcher: AttentionWatcher,
    onOpenFeature: (BuiltIn) -> Unit,
    onMoreApps: () -> Unit,
    onHandoff: (SystemDestination) -> Unit,
    onMissingTile: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalPalette.current

    // The launcher never exits on back.
    BackHandler(enabled = true) {}

    // A minute tick keeps the clock and the day part honest.
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            val msToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(msToNextMinute)
        }
    }

    val attention by produceState(initialValue = AttentionState(), watcher) {
        watcher.state.collect { value = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Masthead(
            clockText = DayPart.clockText(context, now.time),
            dayPartText = DayPart.dayPartText(context, now),
            dateText = DayPart.dateText(now.time),
            isEvening = DayPart.isMoon(now),
        )
        Column(
            modifier = Modifier.padding(
                start = Dimens.screenSide,
                end = Dimens.screenSide,
                top = Dimens.gapColumn,
                bottom = Dimens.screenBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapColumn),
        ) {
            if (attention.anythingRaised) {
                AttentionLamp(items = lampItems(attention, watcher, onHandoff))
            } else {
                StatusPill(text = stringResource(R.string.all_is_well))
            }
            TileGrid(
                layout = layout,
                apps = apps,
                onOpenFeature = onOpenFeature,
                onMissingTile = onMissingTile,
            )
            ApplianceKey(
                label = stringResource(R.string.more_apps),
                onClick = onMoreApps,
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
            )
        }
    }
}

@Composable
private fun TileGrid(
    layout: List<SavedTile>,
    apps: AppsRepository,
    onOpenFeature: (BuiltIn) -> Unit,
    onMissingTile: () -> Unit,
) {
    val density = LocalDensity.current
    val iconPx = with(density) { Dimens.appIcon.roundToPx() }
    val columns = tileColumns()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapTile)) {
        layout.chunked(columns).forEach { row ->
            // IntrinsicSize.Min lets the empty spot match the real tile beside
            // it. Without it the empty spot is frozen at the 128dp floor while
            // a tile with a larger glyph or a scaled label grows past it, and
            // the default home screen rags by tens of dp.
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.gapTile),
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                row.forEach { tile ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        HomeTile(tile, apps, iconPx, onOpenFeature, onMissingTile)
                    }
                }
                repeat(columns - row.size) {
                    Box(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun HomeTile(
    tile: SavedTile,
    apps: AppsRepository,
    iconPx: Int,
    onOpenFeature: (BuiltIn) -> Unit,
    onMissingTile: () -> Unit,
) {
    val feature = BuiltIn.fromId(tile.builtIn)
    when {
        feature != null -> Tile(
            label = feature.label(),
            icon = feature.icon(),
            onClick = { onOpenFeature(feature) },
        )
        tile.packageName != null -> {
            val entry = remember(tile) { apps.entryFor(tile) }
            if (entry != null) {
                val icon = remember(entry.key) { apps.icon(entry, iconPx) }
                Tile(
                    label = entry.label,
                    appIcon = icon,
                    // A launch can fail if the app went away between drawing
                    // this tile and pressing it.
                    onClick = { if (!apps.launch(entry)) onMissingTile() },
                )
            } else {
                // The app is not there right now: uninstalled, or on a profile
                // that is turned off. Drawing nothing removed a tile from the
                // layout the hands learned and said nothing about it.
                Tile(
                    label = stringResource(R.string.tile_app_missing),
                    icon = LineIcons.plus,
                    contentDescription = stringResource(R.string.a11y_tile_app_missing),
                    onClick = onMissingTile,
                )
            }
        }
        else -> EmptySpot()
    }
}

/**
 * An empty spot keeps its place silently on the home screen, and keeps the
 * height of whatever tile sits beside it.
 */
@Composable
private fun EmptySpot() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .defaultMinSize(minHeight = Dimens.tile)
    )
}

@Composable
private fun lampItems(
    state: AttentionState,
    watcher: AttentionWatcher,
    onHandoff: (SystemDestination) -> Unit,
): List<LampItem> {
    val context = LocalContext.current
    val view = LocalView.current
    val items = mutableListOf<LampItem>()
    if (state.ringerSilent || state.ringerVibrate) {
        items += LampItem(
            id = "ringer",
            sentence = stringResource(
                if (state.ringerSilent) R.string.attention_ringer_off
                else R.string.attention_ringer_vibrate
            ),
            icon = LineIcons.bellOff,
            repairLabel = stringResource(R.string.attention_repair_ringer),
            onRepair = {
                // Confirm only if the ringer actually came back on. Otherwise
                // this is a handoff, and saying so with the reject haptic is
                // the honest signal.
                if (watcher.repairRinger()) {
                    Haptics.confirm(view)
                } else {
                    Haptics.reject(view)
                    onHandoff(SystemDestination.SOUND)
                }
            },
        )
    }
    if (state.dndOn) {
        items += LampItem(
            id = "dnd",
            sentence = stringResource(R.string.attention_dnd_on),
            icon = LineIcons.bellQuiet,
            // The key said "Turn Do Not Disturb off" on every device, but the
            // toggle needs a special access this app never requests, so it
            // could never do it. The label now describes whichever of the two
            // things will actually happen.
            repairLabel = stringResource(
                if (watcher.canToggleDnd()) R.string.attention_repair_dnd
                else R.string.attention_repair_dnd_open
            ),
            onRepair = {
                if (watcher.repairDnd()) {
                    Haptics.confirm(view)
                } else {
                    Haptics.tap(view)
                    onHandoff(SystemDestination.DND)
                }
            },
        )
    }
    if (state.airplaneOn) {
        items += LampItem(
            id = "airplane",
            sentence = stringResource(R.string.attention_airplane_on),
            icon = LineIcons.airplane,
            repairLabel = stringResource(R.string.attention_repair_airplane),
            onRepair = {
                Haptics.tap(view)
                onHandoff(SystemDestination.AIRPLANE)
            },
        )
    } else if (state.noNetwork) {
        items += LampItem(
            id = "network",
            sentence = stringResource(R.string.attention_no_network),
            icon = LineIcons.noNetwork,
            repairLabel = stringResource(R.string.attention_repair_network),
            onRepair = {
                Haptics.tap(view)
                onHandoff(SystemDestination.NETWORK)
            },
        )
    }
    if (state.batteryLow) {
        items += LampItem(
            id = "battery",
            sentence = stringResource(R.string.attention_battery_low, state.batteryPercent),
            icon = LineIcons.battery,
        )
    }
    if (state.storageNearlyFull) {
        items += LampItem(
            id = "storage",
            sentence = stringResource(R.string.attention_storage_full),
            icon = LineIcons.storage,
            repairLabel = stringResource(R.string.attention_repair_storage),
            onRepair = {
                Haptics.tap(view)
                onHandoff(SystemDestination.STORAGE)
            },
        )
    }
    if (state.batteryOptimizationOn) {
        items += LampItem(
            id = "battery_optimization",
            sentence = stringResource(R.string.attention_battery_optimization),
            icon = LineIcons.sleep,
            repairLabel = stringResource(R.string.attention_repair_battery_optimization),
            onRepair = {
                Haptics.tap(view)
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:${context.packageName}"),
                )
                context.startActivity(intent)
            },
        )
    }
    return items
}
