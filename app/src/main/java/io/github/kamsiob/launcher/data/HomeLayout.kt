package io.github.kamsiob.launcher.data

/**
 * Every edit the home layout can undergo, in one place, so the screens that
 * offer them and the tests that check them cannot drift apart.
 *
 * Two rules run through all of it. The grid never shrinks: taking an app off
 * leaves an empty spot holding its place, because a layout that reflows is a
 * layout the hands have to learn again. And Call always stays first.
 */
object HomeLayout {

    fun isLocked(tile: SavedTile): Boolean = tile.builtIn == BuiltIn.CALL.id

    /** A new app takes the first empty spot, or lands at the bottom. */
    fun add(layout: List<SavedTile>, tile: SavedTile): List<SavedTile> {
        val emptyIndex = layout.indexOfFirst { it.isEmpty }
        return if (emptyIndex >= 0) {
            layout.toMutableList().apply { this[emptyIndex] = tile }
        } else {
            layout + tile
        }
    }

    /** Two tiles trade places. Nothing else moves. */
    fun swap(layout: List<SavedTile>, a: Int, b: Int): List<SavedTile> =
        layout.toMutableList().apply {
            val held = this[a]
            this[a] = this[b]
            this[b] = held
        }

    /** Jumps a tile to the top spot, or to second when Call holds the top. */
    fun putFirst(layout: List<SavedTile>, index: Int): List<SavedTile> {
        if (layout.isEmpty() || isLocked(layout[index])) return layout
        return layout.toMutableList().apply {
            val app = removeAt(index)
            add(if (isNotEmpty() && isLocked(first())) 1 else 0, app)
        }
    }

    /** The tile becomes an empty spot. Nothing is ever uninstalled. */
    fun takeOff(layout: List<SavedTile>, index: Int): List<SavedTile> {
        if (isLocked(layout[index])) return layout
        return layout.toMutableList().apply { this[index] = SavedTile.EMPTY }
    }
}
