package io.github.kamsiob.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity

/**
 * How many tile columns the home grid uses.
 *
 * The grid specifies two columns of 128dp tiles. At large font scales a word
 * like "Magnifier" cannot fit that width at a size this audience can read, and
 * the two bad answers are breaking it mid word or shrinking it below the point
 * of the app. So the grid drops to one column instead. The order of the tiles
 * never changes, which is what the hands actually learn, and every label keeps
 * its full size.
 */
@Composable
@ReadOnlyComposable
fun tileColumns(): Int {
    val scale = LocalDensity.current.fontScale * LocalTextStep.current.multiplier
    return if (scale >= 1.5f) 1 else 2
}
