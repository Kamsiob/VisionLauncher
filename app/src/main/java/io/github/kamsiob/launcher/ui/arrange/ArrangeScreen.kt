package io.github.kamsiob.launcher.ui.arrange

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.AppsRepository
import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.data.HomeLayout
import io.github.kamsiob.launcher.data.SavedTile
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.PromptBar
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.SerifHeading
import io.github.kamsiob.launcher.ui.components.Tile
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.components.UndoStrip
import io.github.kamsiob.launcher.ui.home.icon
import io.github.kamsiob.launcher.ui.home.label
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.tileColumns
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import kotlinx.coroutines.delay

/** What arranging mode is currently doing. */
private sealed interface ArrangeMode {
    data object Browsing : ArrangeMode
    data class Chosen(val index: Int) : ArrangeMode
    data class Moving(val index: Int) : ArrangeMode
}

/**
 * Grid 16 through 19. Arranging happens on the real home grid, taps only. The
 * Done chip never leaves the top, so nobody is ever stranded in the mode.
 */
@Composable
fun ArrangeScreen(
    startingLayout: List<SavedTile>,
    apps: AppsRepository,
    onKeep: (List<SavedTile>) -> Unit,
    onExit: () -> Unit,
) {
    val view = LocalView.current
    var working by remember(startingLayout) { mutableStateOf(startingLayout) }
    var mode by remember { mutableStateOf<ArrangeMode>(ArrangeMode.Browsing) }
    var undo by remember { mutableStateOf<UndoState?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var addingApp by remember { mutableStateOf(false) }
    var keptOnExit by remember { mutableStateOf(false) }

    // Pressing Home mid arrange keeps completed changes and says so.
    val exitKeepingChanges = {
        if (working != startingLayout) {
            onKeep(working)
            keptOnExit = true
        } else {
            onExit()
        }
    }
    LaunchedEffect(keptOnExit) {
        if (keptOnExit) {
            delay(2200)
            onExit()
        }
    }

    // Back inside the mode steps out of the current choice rather than the
    // whole screen, so a stray press never loses work.
    BackHandler(enabled = mode !is ArrangeMode.Browsing) {
        mode = ArrangeMode.Browsing
    }

    if (addingApp) {
        AddAppScreen(
            currentLayout = working,
            apps = apps,
            onAdd = { tile ->
                working = HomeLayout.add(working, tile)
                addingApp = false
                Haptics.confirm(view)
            },
            onHome = exitKeepingChanges,
            onBack = { addingApp = false },
        )
        return
    }

    if (showPreview) {
        KeepItScreen(
            layout = working,
            apps = apps,
            onKeep = {
                onKeep(working)
                onExit()
            },
            onRevert = onExit,
            onHome = exitKeepingChanges,
        )
        return
    }

    val chosenIndex = (mode as? ArrangeMode.Chosen)?.index
    val movingIndex = (mode as? ArrangeMode.Moving)?.index
    val liftedIndex = chosenIndex ?: movingIndex

    val promptText = when {
        movingIndex != null -> stringResource(
            R.string.arrange_prompt_destination,
            tileLabel(working[movingIndex], apps),
        )
        else -> stringResource(R.string.arrange_prompt)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    ScreenFrame {
        PromptBar(
            text = promptText,
            onDone = {
                if (working == startingLayout) onExit() else showPreview = true
            },
            dimmed = chosenIndex != null,
        )
        ArrangeGrid(
            layout = working,
            apps = apps,
            liftedIndex = liftedIndex,
            dimOthers = chosenIndex != null,
            onTapTile = { index ->
                when {
                    movingIndex != null -> {
                        when {
                            index == movingIndex -> {
                                // Tapping the lifted app again cancels, no penalty.
                                mode = ArrangeMode.Browsing
                                Haptics.tap(view)
                            }
                            isLocked(working[index]) -> {
                                // Call is not a destination either. Stay in the
                                // move so a different spot can be chosen.
                                Haptics.reject(view)
                                undo = UndoState.CallLocked
                            }
                            else -> {
                                val before = working
                                working = HomeLayout.swap(working, movingIndex, index)
                                undo = UndoState.Moved(before)
                                mode = ArrangeMode.Browsing
                                Haptics.confirm(view)
                            }
                        }
                    }
                    isLocked(working[index]) -> {
                        Haptics.reject(view)
                        mode = ArrangeMode.Browsing
                        undo = UndoState.CallLocked
                    }
                    working[index].isEmpty -> addingApp = true
                    else -> {
                        mode = ArrangeMode.Chosen(index)
                        Haptics.tap(view)
                    }
                }
            },
        )

        when (val current = undo) {
            is UndoState.Moved -> ApplianceKey(
                label = stringResource(R.string.arrange_undo_last),
                onClick = {
                    working = current.before
                    undo = null
                },
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
                committing = true,
            )
            is UndoState.TakenOff -> UndoStrip(
                message = stringResource(R.string.arrange_taken_off, current.label),
                actionLabel = stringResource(R.string.put_it_back),
                onAction = {
                    working = current.before
                    undo = null
                },
            )
            UndoState.CallLocked -> NoteText(stringResource(R.string.arrange_call_stays))
            null -> {}
        }

        ApplianceKey(
            label = stringResource(R.string.arrange_add_app),
            onClick = { addingApp = true },
            minHeight = Dimens.keySmall,
            fontSize = TypeScale.keyLabelSmall,
        )
        if (keptOnExit) {
            NoteText(stringResource(R.string.arrange_kept_partial))
        }
    }

    if (chosenIndex != null) {
        val chosenLabel = tileLabel(working[chosenIndex], apps)
        ActionSheet(
            label = chosenLabel,
            onMove = { mode = ArrangeMode.Moving(chosenIndex) },
            onPutFirst = {
                val before = working
                working = HomeLayout.putFirst(working, chosenIndex)
                undo = UndoState.Moved(before)
                mode = ArrangeMode.Browsing
                Haptics.confirm(view)
            },
            onTakeOff = {
                val before = working
                working = HomeLayout.takeOff(working, chosenIndex)
                undo = UndoState.TakenOff(before, chosenLabel)
                mode = ArrangeMode.Browsing
                Haptics.confirm(view)
            },
            onNeverMind = { mode = ArrangeMode.Browsing },
        )
    }
    }
}

private sealed interface UndoState {
    data class Moved(val before: List<SavedTile>) : UndoState
    data class TakenOff(val before: List<SavedTile>, val label: String) : UndoState
    data object CallLocked : UndoState
}

/** Call cannot be moved or removed. */
private fun isLocked(tile: SavedTile): Boolean = HomeLayout.isLocked(tile)

@Composable
private fun tileLabel(tile: SavedTile, apps: AppsRepository): String {
    val feature = BuiltIn.fromId(tile.builtIn)
    return when {
        feature != null -> feature.label()
        tile.packageName != null -> remember(tile) { apps.entryFor(tile) }?.label ?: ""
        else -> stringResource(R.string.empty_spot)
    }
}

@Composable
private fun ArrangeGrid(
    layout: List<SavedTile>,
    apps: AppsRepository,
    liftedIndex: Int?,
    dimOthers: Boolean,
    onTapTile: (Int) -> Unit,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val iconPx = with(density) { Dimens.appIcon.roundToPx() }
    val columns = tileColumns()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapTile)) {
        layout.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.gapTile),
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                row.forEachIndexed { columnIndex, tile ->
                    val index = rowIndex * columns + columnIndex
                    val lifted = index == liftedIndex
                    val dimmed = dimOthers && !lifted
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(if (dimmed) Modifier.alpha(0.38f) else Modifier)
                            .then(
                                if (lifted) {
                                    Modifier.border(
                                        Dimens.liftedRing,
                                        palette.accent,
                                        RoundedCornerShape(Dimens.radiusKey + Dimens.liftedRingOffset),
                                    ).padding(Dimens.liftedRingOffset)
                                } else {
                                    Modifier
                                }
                            ),
                    ) {
                        ArrangeTile(
                            tile = tile,
                            apps = apps,
                            iconPx = iconPx,
                            lifted = lifted,
                            onClick = { onTapTile(index) },
                        )
                    }
                }
                repeat(columns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ArrangeTile(
    tile: SavedTile,
    apps: AppsRepository,
    iconPx: Int,
    lifted: Boolean,
    onClick: () -> Unit,
) {
    val feature = BuiltIn.fromId(tile.builtIn)
    val label = tileLabel(tile, apps)
    val description = when {
        lifted -> stringResource(R.string.a11y_arrange_lifted, label)
        isLocked(tile) -> stringResource(R.string.a11y_arrange_locked)
        tile.isEmpty -> stringResource(R.string.a11y_arrange_empty)
        else -> stringResource(R.string.a11y_arrange_tile, label)
    }
    when {
        feature != null -> Tile(
            label = label,
            icon = feature.icon(),
            contentDescription = description,
            onClick = onClick,
        )
        tile.packageName != null -> {
            val entry = remember(tile) { apps.entryFor(tile) }
            Tile(
                label = label,
                appIcon = entry?.let { apps.icon(it, iconPx) },
                contentDescription = description,
                onClick = onClick,
            )
        }
        else -> Tile(
            label = stringResource(R.string.empty_spot),
            icon = LineIcons.plus,
            contentDescription = description,
            onClick = onClick,
        )
    }
}

/** Grid 17. Target first, then action. */
@Composable
private fun ActionSheet(
    label: String,
    onMove: () -> Unit,
    onPutFirst: () -> Unit,
    onTakeOff: () -> Unit,
    onNeverMind: () -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.screenSide),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(Dimens.radiusSheet))
                .background(palette.card, RoundedCornerShape(Dimens.radiusSheet))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        ) {
            Text(
                text = label,
                style = bodyStyle(size = TypeScale.tileLabel, weight = FontWeight.ExtraBold, lineHeightFactor = 1.2f),
                color = palette.text,
            )
            ApplianceKey(
                label = stringResource(R.string.arrange_move_it),
                onClick = onMove,
                style = KeyStyle.PRIMARY,
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
            )
            ApplianceKey(
                label = stringResource(R.string.arrange_put_first),
                onClick = onPutFirst,
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
                committing = true,
            )
            ApplianceKey(
                label = stringResource(R.string.arrange_take_off),
                onClick = onTakeOff,
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
                committing = true,
            )
            ApplianceKey(
                label = stringResource(R.string.key_never_mind),
                onClick = onNeverMind,
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
            )
        }
    }
}

/** Grid 19. The miniature preview, then Keep it or put it back. */
@Composable
private fun KeepItScreen(
    layout: List<SavedTile>,
    apps: AppsRepository,
    onKeep: () -> Unit,
    onRevert: () -> Unit,
    onHome: () -> Unit,
) {
    val palette = LocalPalette.current
    ScreenFrame(topBar = { TopBar(onHome = onHome) }) {
        SerifHeading(stringResource(R.string.arrange_keep_title))
        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .align(Alignment.CenterHorizontally)
                .background(palette.background, RoundedCornerShape(18.dp))
                .border(1.dp, palette.outline, RoundedCornerShape(18.dp))
                .padding(bottom = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.masthead)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "10:24",
                    style = io.github.kamsiob.launcher.ui.theme.serifStyle(size = 32, lineHeightFactor = 1f),
                    color = palette.mastheadText,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                layout.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { tile ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(palette.card, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 2.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = tileLabel(tile, apps),
                                    style = bodyStyle(size = 13, weight = FontWeight.Bold, lineHeightFactor = 1.1f),
                                    color = palette.text,
                                )
                            }
                        }
                        if (row.size == 1) Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        ApplianceKey(
            label = stringResource(R.string.arrange_keep_it),
            onClick = onKeep,
            style = KeyStyle.PRIMARY,
            minHeight = Dimens.keepKey,
            committing = true,
        )
        ApplianceKey(
            label = stringResource(R.string.arrange_revert),
            onClick = onRevert,
            minHeight = Dimens.keepKey,
        )
    }
}
