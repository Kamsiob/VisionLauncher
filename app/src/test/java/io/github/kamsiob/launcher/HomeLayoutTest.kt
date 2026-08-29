package io.github.kamsiob.launcher

import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.data.HomeLayout
import io.github.kamsiob.launcher.data.SavedTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The promises the arranging screens make out loud, checked against the code
 * those screens actually call.
 */
class HomeLayoutTest {

    private val defaultLayout = listOf(
        SavedTile.of(BuiltIn.CALL),
        SavedTile.of(BuiltIn.MESSAGES),
        SavedTile.of(BuiltIn.MAGNIFIER),
        SavedTile.of(BuiltIn.CAMERA),
        SavedTile.of(BuiltIn.PHOTOS),
        SavedTile.EMPTY,
    )

    @Test
    fun `a new app takes the first empty spot`() {
        val result = HomeLayout.add(defaultLayout, SavedTile.ofApp("com.example", "Main"))
        assertEquals(6, result.size)
        assertEquals("com.example", result[5].packageName)
    }

    @Test
    fun `a new app lands at the bottom when the grid is full, as the screen says`() {
        val full = defaultLayout.dropLast(1) + SavedTile.ofApp("com.first", "Main")
        val result = HomeLayout.add(full, SavedTile.ofApp("com.second", "Main"))
        assertEquals(7, result.size)
        assertEquals("com.second", result.last().packageName)
    }

    @Test
    fun `trading two tiles swaps exactly those two and nothing else`() {
        val result = HomeLayout.swap(defaultLayout, 1, 4)
        assertEquals(BuiltIn.PHOTOS.id, result[1].builtIn)
        assertEquals(BuiltIn.MESSAGES.id, result[4].builtIn)
        assertEquals(defaultLayout[0], result[0])
        assertEquals(defaultLayout[2], result[2])
        assertEquals(defaultLayout[3], result[3])
        assertEquals(defaultLayout[5], result[5])
    }

    @Test
    fun `put it first moves an app to the top spot below Call`() {
        val result = HomeLayout.putFirst(defaultLayout, 4)
        assertEquals(BuiltIn.CALL.id, result[0].builtIn)
        assertEquals(BuiltIn.PHOTOS.id, result[1].builtIn)
        assertEquals(defaultLayout.size, result.size)
    }

    @Test
    fun `put it first takes the top spot when Call is not on the screen`() {
        val withoutCall = defaultLayout.drop(1)
        val result = HomeLayout.putFirst(withoutCall, 3)
        assertEquals(BuiltIn.PHOTOS.id, result[0].builtIn)
    }

    @Test
    fun `taking an app off leaves an empty spot rather than shrinking the grid`() {
        val result = HomeLayout.takeOff(defaultLayout, 2)
        assertEquals(defaultLayout.size, result.size)
        assertTrue(result[2].isEmpty)
        assertEquals(BuiltIn.CAMERA.id, result[3].builtIn)
    }

    @Test
    fun `Call cannot be taken off`() {
        assertEquals(defaultLayout, HomeLayout.takeOff(defaultLayout, 0))
    }

    @Test
    fun `Call cannot be put first, because it already is`() {
        assertEquals(defaultLayout, HomeLayout.putFirst(defaultLayout, 0))
    }

    @Test
    fun `Call is locked and nothing else is`() {
        assertTrue(HomeLayout.isLocked(SavedTile.of(BuiltIn.CALL)))
        assertFalse(HomeLayout.isLocked(SavedTile.of(BuiltIn.MESSAGES)))
        assertFalse(HomeLayout.isLocked(SavedTile.EMPTY))
        assertFalse(HomeLayout.isLocked(SavedTile.ofApp("com.example", "Main")))
    }

    @Test
    fun `Call cannot be traded away by choosing it as a destination`() {
        // The screen says "Call always stays first". Guarding only the tile
        // being picked up left the destination open, so moving Photos onto
        // Call's spot pushed Call to position five.
        assertEquals(defaultLayout, HomeLayout.swap(defaultLayout, 4, 0))
        assertEquals(defaultLayout, HomeLayout.swap(defaultLayout, 0, 4))
    }

    @Test
    fun `Call stays first through any sequence of permitted edits`() {
        var layout = defaultLayout
        layout = HomeLayout.swap(layout, 1, 4)
        layout = HomeLayout.putFirst(layout, 3)
        layout = HomeLayout.takeOff(layout, 2)
        layout = HomeLayout.add(layout, SavedTile.ofApp("com.example", "Main"))
        layout = HomeLayout.swap(layout, 2, 5)
        assertEquals(
            "no permitted edit may move Call out of first place",
            BuiltIn.CALL.id,
            layout[0].builtIn,
        )
    }

    @Test
    fun `every edit is reversible by keeping the layout that came before it`() {
        val afterMove = HomeLayout.swap(defaultLayout, 1, 4)
        val afterTakeOff = HomeLayout.takeOff(afterMove, 2)
        assertEquals(afterMove, HomeLayout.swap(HomeLayout.swap(afterMove, 1, 4), 1, 4))
        assertTrue(afterTakeOff[2].isEmpty)
        assertEquals(defaultLayout, HomeLayout.swap(afterMove, 1, 4))
    }
}
