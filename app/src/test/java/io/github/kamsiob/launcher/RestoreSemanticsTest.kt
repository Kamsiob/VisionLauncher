package io.github.kamsiob.launcher

import io.github.kamsiob.launcher.data.BuiltIn
import io.github.kamsiob.launcher.data.SavedTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Put my screen back" has to actually put the screen back.
 *
 * The first implementation wrote the snapshot and the current layout to the
 * same value on every Keep, which made restoring a provable no operation while
 * the screen still announced "Your home screen is back the way it was". A false
 * confirmation is the worst defect this app can have, because every other
 * promise it makes rests on the interface telling the truth.
 *
 * These tests model the store's two writes without Android, so the semantics
 * are pinned even though DataStore itself is not exercised here.
 */
class RestoreSemanticsTest {

    /** The two values LayoutStore persists. */
    private data class Store(
        var current: List<SavedTile>,
        var snapshot: List<SavedTile>? = null,
    ) {
        /** Mirrors LayoutStore.keep. */
        fun keep(tiles: List<SavedTile>) {
            snapshot = current
            current = tiles
        }

        /** Mirrors LayoutStore.restoreSnapshot, including its return value. */
        fun restore(): Boolean {
            val snap = snapshot ?: return false
            if (snap == current) return false
            current = snap
            return true
        }
    }

    private val original = listOf(
        SavedTile.of(BuiltIn.CALL),
        SavedTile.of(BuiltIn.MESSAGES),
        SavedTile.of(BuiltIn.MAGNIFIER),
        SavedTile.of(BuiltIn.CAMERA),
        SavedTile.of(BuiltIn.PHOTOS),
        SavedTile.EMPTY,
    )

    private val rearranged = listOf(
        SavedTile.of(BuiltIn.CALL),
        SavedTile.of(BuiltIn.PHOTOS),
        SavedTile.of(BuiltIn.MESSAGES),
        SavedTile.of(BuiltIn.MAGNIFIER),
        SavedTile.of(BuiltIn.CAMERA),
        SavedTile.EMPTY,
    )

    @Test
    fun `putting the screen back undoes the last kept change`() {
        val store = Store(current = original)
        store.keep(rearranged)
        assertEquals(rearranged, store.current)

        assertTrue("restore must report that it changed something", store.restore())
        assertEquals("the screen must actually be back the way it was", original, store.current)
    }

    @Test
    fun `restoring twice is harmless and honest the second time`() {
        val store = Store(current = original)
        store.keep(rearranged)

        assertTrue(store.restore())
        assertFalse("nothing left to undo, so it must not claim otherwise", store.restore())
        assertEquals(original, store.current)
    }

    @Test
    fun `restoring before anything was ever kept changes nothing and says so`() {
        val store = Store(current = original)
        assertFalse(store.restore())
        assertEquals(original, store.current)
    }

    @Test
    fun `a second keep moves the restore point forward by one session`() {
        val twiceChanged = rearranged.dropLast(1) + SavedTile.ofApp("com.example", "Main")
        val store = Store(current = original)

        store.keep(rearranged)
        store.keep(twiceChanged)

        assertTrue(store.restore())
        assertEquals(
            "restore undoes the most recent session, not every session",
            rearranged,
            store.current,
        )
    }

    @Test
    fun `keeping the same layout twice leaves nothing to undo`() {
        val store = Store(current = original)
        store.keep(original)
        assertFalse("nothing changed, so there is nothing to put back", store.restore())
        assertEquals(original, store.current)
    }
}
