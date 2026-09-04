package io.github.kamsiob.launcher

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kamsiob.launcher.seeing.Reading
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What comes back from a photograph bad enough to make the recognizer guess.
 *
 * The reader was reported reading out characters that were never on the page.
 * A clean render never produces those; a speckled, shadowed, slightly blurred
 * photograph does, which is what a phone held over a bottle actually takes.
 */
@RunWith(AndroidJUnit4::class)
class NoiseTest {

    @Test
    fun a_noisy_photograph_gives_back_words_and_not_marks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets
            .open("label-noisy.jpg").use { it.readBytes() }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val text = Reading.read(context, bitmap)
        val spoken = Reading.forSpeaking(text.orEmpty())
        android.util.Log.i("NOISE", "read=[${text.orEmpty().replace("\n", " | ")}]")
        android.util.Log.i("NOISE", "spoken=[$spoken]")

        assertTrue("nothing was recognized at all", !text.isNullOrBlank())
        assertTrue("lost the drug name", text!!.contains("IBUPROFEN", true))
        // Nothing a voice would read out by name may survive into speech.
        val named = "|~<>[]{}\\/@#^*_=+".toSet()
        val leaked = spoken.filter { it in named }
        assertTrue("marks a voice would name leaked into speech: $leaked", leaked.isEmpty())
    }
}
