package io.github.kamsiob.launcher.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Every value here is read from design/design-grid-v4.html at 1px equals 1dp.
 */
object Dimens {
    // Screen padding: 28dp top, 24dp sides, 30dp bottom.
    val screenTop = 28.dp
    val screenSide = 24.dp
    val screenBottom = 30.dp

    // Gaps between keys: 12 to 14dp. Tiles use 14, key stacks use 12,
    // the screen column uses 16 as in the grid's flex gap.
    val gap = 12.dp
    val gapTile = 14.dp
    val gapColumn = 16.dp

    // Corner radii.
    val radiusKey = 20.dp
    val radiusLamp = 20.dp
    val radiusUndo = 16.dp
    val radiusPromptBar = 16.dp
    val radiusSheet = 22.dp
    val radiusDoneChip = 12.dp
    val radiusPill = 999.dp

    // Touch floors. All far above Android's 48dp minimum, deliberately.
    val keyMin = 88.dp
    val keySmall = 72.dp
    val keypadKey = 96.dp
    val rowKey = 94.dp
    val homeKey = 76.dp
    val tile = 128.dp
    val bigKey = 130.dp
    val keepKey = 104.dp
    val phraseKey = 84.dp
    val dismissKey = 60.dp

    // Element sizes.
    val tileIcon = 46.dp
    val appIcon = 52.dp
    val appIconRadius = 14.dp
    val avatar = 60.dp
    val rowIcon = 36.dp
    val homeKeyIcon = 28.dp
    val lampIcon = 32.dp
    val daylineIcon = 32.dp
    val statusDot = 15.dp
    val thresholdIcon = 64.dp

    // Borders.
    val outlinedBorder = 3.dp
    val statusBorder = 2.dp
    val liftedRing = 4.dp
    val liftedRingOffset = 3.dp
}
