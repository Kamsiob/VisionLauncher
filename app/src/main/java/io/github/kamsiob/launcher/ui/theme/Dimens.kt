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
    /** The phrase keys on the reply screen, grid 09. */
    val phraseKey = 84.dp
    val dismissKey = 60.dp
    // The grid draws .themecard at min-height 112px.
    val themeCard = 112.dp
    /** The frozen frame above the recognized words, grid 11 draws it at 220px. */
    val readerFrame = 220.dp

    // Element sizes.
    //
    // The icons were enlarged from the grid's own values after the user looked
    // at the running app and found them small, three times now: twice early on
    // and once more, by 15 percent, after using the app on a real phone. See DECISIONS.md D34: the grid
    // sized them by the conventions of ordinary interface design while sizing
    // its touch targets by the aging literature, and only the targets got the
    // argument. These are the corrected values, and the grid now carries them.

    // Both kinds of tile glyph sit in one slot of this height, so a built in
    // line icon and a third party bitmap produce tiles of identical height.
    // Without the shared slot the two differ and the rows rag.
    val tileIconSlot = 92.dp
    val tileIcon = 92.dp

    // Deliberately smaller than the line icon and unchanged from the grid. The
    // size difference is the quiet cue that tells built in features from
    // installed apps, which DESIGN.md relies on.
    val appIcon = 69.dp
    val appIconRadius = 16.dp
    val avatar = 69.dp

    // The grid draws two row icon sizes, 40 on the Call screen and 36 on rows
    // carrying a mono metadata line, and the first port collapsed them to 36.
    // Both are restored, both enlarged.
    val rowIcon = 74.dp
    val rowIconWithMeta = 67.dp

    val homeKeyIcon = 51.dp

    // Keys carry an icon sized to the key. One value served the 72dp, 88dp and
    // 130dp keys before, which is how the grid's own 30 and 40 both became 32.
    val keyIconSmall = 46.dp
    val keyIcon = 55.dp
    val keyIconBig = 74.dp

    // The lamp glyph outranks the day part mark on purpose. It backs a colored
    // signal that must never carry meaning alone, while the sun and moon are
    // decoration that happens to aid orientation. They were equal by accident.
    val lampIcon = 55.dp
    val daylineIcon = 48.dp

    val statusDot = 20.dp
    val thresholdIcon = 101.dp

    // Borders.
    val outlinedBorder = 3.dp
    val statusBorder = 2.dp
    val liftedRing = 4.dp
    val liftedRingOffset = 3.dp
}
