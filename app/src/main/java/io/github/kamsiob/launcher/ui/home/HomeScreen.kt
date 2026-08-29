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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.attention.AttentionState
import io.github.kamsiob.launcher.attention.AttentionWatcher
import io.github.kamsiob.launcher.data.AppsRepository
import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.data.SavedTile
import io.github.kamsiob.launcher.home.DayPart
import io.github.kamsiob.launcher.nav.SystemDestination
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
            TileGrid(layout = layout, apps = apps, onOpenFeature = onOpenFeature)
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
) {
    val density = LocalDensity.current
    val iconPx = with(density) { Dimens.appIcon.roundToPx() }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapTile)) {
        layout.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gapTile)) {
                row.forEach { tile ->
                    Box(modifier = Modifier.weight(1f)) {
                        HomeTile(tile, apps, iconPx, onOpenFeature)
                    }
                }
                if (row.size == 1) {
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
                    onClick = { apps.launch(entry) },
                )
            } else {
                // The app behind this tile is gone. The spot stays; arranging
                // mode is where it gets refilled.
                EmptySpot()
            }
        }
        else -> EmptySpot()
    }
}

/** An empty spot keeps its place silently on the home screen. */
@Composable
private fun EmptySpot() {
    Box(modifier = Modifier.defaultMinSize(minHeight = Dimens.tile))
}

@Composable
private fun lampItems(
    state: AttentionState,
    watcher: AttentionWatcher,
    onHandoff: (SystemDestination) -> Unit,
): List<LampItem> {
    val context = LocalContext.current
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
                if (!watcher.repairRinger()) onHandoff(SystemDestination.SOUND)
            },
        )
    }
    if (state.dndOn) {
        items += LampItem(
            id = "dnd",
            sentence = stringResource(R.string.attention_dnd_on),
            icon = LineIcons.bellQuiet,
            repairLabel = stringResource(R.string.attention_repair_dnd),
            onRepair = {
                if (!watcher.repairDnd()) onHandoff(SystemDestination.SOUND)
            },
        )
    }
    if (state.airplaneOn) {
        items += LampItem(
            id = "airplane",
            sentence = stringResource(R.string.attention_airplane_on),
            icon = LineIcons.airplane,
            repairLabel = stringResource(R.string.attention_repair_airplane),
            onRepair = { onHandoff(SystemDestination.AIRPLANE) },
        )
    } else if (state.noNetwork) {
        items += LampItem(
            id = "network",
            sentence = stringResource(R.string.attention_no_network),
            icon = LineIcons.noNetwork,
            repairLabel = stringResource(R.string.attention_repair_network),
            onRepair = { onHandoff(SystemDestination.NETWORK) },
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
            onRepair = { onHandoff(SystemDestination.STORAGE) },
        )
    }
    if (state.batteryOptimizationOn) {
        items += LampItem(
            id = "battery_optimization",
            sentence = stringResource(R.string.attention_battery_optimization),
            icon = LineIcons.sleep,
            repairLabel = stringResource(R.string.attention_repair_battery_optimization),
            onRepair = {
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
