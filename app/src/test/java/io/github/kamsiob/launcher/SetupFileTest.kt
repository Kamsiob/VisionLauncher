package io.github.kamsiob.launcher

import io.github.kamsiob.launcher.data.EmergencyContact
import io.github.kamsiob.launcher.data.Favorite
import io.github.kamsiob.launcher.data.SavedTile
import io.github.kamsiob.launcher.data.Setup
import io.github.kamsiob.launcher.today.TodayCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupFileTest {

    private val full = Setup(
        favorites = listOf(Favorite("Sarah", "5551234", "daughter")),
        emergencyContact = EmergencyContact("Michael", "5555678"),
        replyPhrases = listOf("Yes", "No"),
        todayCards = listOf(
            TodayCard(1, 9, 0, "Blood pressure pill", doneAtMinutes = 545, doneOnDay = 2026241)
        ),
        look = "DARK",
        outlined = true,
        textStep = 2,
        homeLayout = listOf(SavedTile.of(io.github.kamsiob.launcher.data.BuiltIn.CALL)),
    )

    @Test
    fun `a setup survives a round trip`() {
        val back = Setup.read(Setup.write(full))
        assertEquals(full.favorites, back?.favorites)
        assertEquals(full.emergencyContact, back?.emergencyContact)
        assertEquals(full.replyPhrases, back?.replyPhrases)
        assertEquals(full.look, back?.look)
        assertEquals(full.outlined, back?.outlined)
        assertEquals(full.textStep, back?.textStep)
        assertEquals(full.homeLayout, back?.homeLayout)
    }

    @Test
    fun `a card travels without whether it was done`() {
        val back = Setup.read(Setup.write(full))!!
        val card = back.todayCards.single()
        assertEquals("Blood pressure pill", card.what)
        assertEquals(9, card.hour)
        assertNull("a completion must not travel in a setup file", card.doneAtMinutes)
        assertNull(card.doneOnDay)
    }

    @Test
    fun `something that is not a setup file is refused`() {
        assertNull(Setup.read("this is not json"))
        assertNull(Setup.read(""))
        assertNull(Setup.read("{\"unrelated\": true}")?.favorites?.takeIf { it.isNotEmpty() })
    }

    @Test
    fun `a file from a newer version is refused rather than half read`() {
        val fromFuture = Setup.write(full).replace("\"version\": 1", "\"version\": 99")
        assertNull(
            "a newer file may mean fields differently, so it must not be applied",
            Setup.read(fromFuture),
        )
    }

    @Test
    fun `an unknown field from a newer version does not stop the rest loading`() {
        val withExtra = Setup.write(full).replace(
            "\"version\": 1", "\"version\": 1,\n    \"somethingNew\": \"x\""
        )
        val back = Setup.read(withExtra)
        assertTrue("known fields must still load", back?.favorites?.isNotEmpty() == true)
    }
}
