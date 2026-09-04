package io.github.kamsiob.launcher

import io.github.kamsiob.launcher.seeing.Reading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * What the reader says out loud.
 *
 * A voice reads a stray bracket as the word "bracket". Recognition on a real
 * photograph always finds a few marks that were never on the page, and reading
 * those aloud is worse than reading nothing, because somebody listening has no
 * way to tell the invented part from the real one.
 */
class ReadingSpeechTest {

    @Test
    fun `each line becomes its own sentence`() {
        assertEquals(
            "AMOXICILLIN 500 mg. Take one capsule. three times daily",
            Reading.forSpeaking("AMOXICILLIN 500 mg\nTake one capsule\nthree times daily"),
        )
    }

    @Test
    fun `marks a voice would name are not spoken`() {
        val spoken = Reading.forSpeaking("Take one |~ capsule <> daily")
        assertEquals("Take one capsule daily", spoken)
        listOf("|", "~", "<", ">").forEach {
            assertFalse("$it must not survive into speech", spoken.contains(it))
        }
    }

    @Test
    fun `ordinary sentence punctuation survives`() {
        assertEquals(
            "Dr. Reyes, 2 p.m. 50% off",
            Reading.forSpeaking("Dr. Reyes, 2 p.m.\n50% off"),
        )
    }

    @Test
    fun `a line of nothing but marks is dropped rather than read`() {
        assertEquals("Take one capsule", Reading.forSpeaking("Take one capsule\n|||\n~~~"))
    }

    @Test
    fun `blank input says nothing`() {
        assertEquals("", Reading.forSpeaking("   \n\n  "))
    }
}
