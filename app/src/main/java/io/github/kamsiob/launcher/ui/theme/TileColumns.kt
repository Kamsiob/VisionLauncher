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
fun tileColumns(): Int = if (sideBySideFits()) 2 else 1

/**
 * The single threshold every side by side layout obeys: the tile grid, the
 * Home and Back bar, and the three Look cards in Settings.
 *
 * It is one number so the reflow is one thing the hands learn rather than
 * three. It sits at 1.25, just under the user's own largest text step, because
 * above that a two across layout starts shrinking labels to fit: "Home" beside
 * a 44dp glyph runs out of room at 1.32, and a tile label begins auto sizing
 * around the same place. Reflowing before anything shrinks means the size the
 * person chose is the size they get. Raising an icon lowers that number, so
 * recompute it whenever one grows.
 */
@Composable
@ReadOnlyComposable
fun sideBySideFits(): Boolean {
    val scale = LocalDensity.current.fontScale * LocalTextStep.current.multiplier
    return scale < 1.25f
}
