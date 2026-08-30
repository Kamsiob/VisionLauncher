package io.github.kamsiob.launcher

import android.speech.SpeechRecognizer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kamsiob.launcher.messages.Dictation
import io.github.kamsiob.launcher.support.Speaking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Whether this phone can actually take a spoken reply and read a message out.
 *
 * Both were written against an emulator that has neither, so the reply screen
 * has never been shown to a device that does. This does not speak into the
 * microphone, which no test can; it checks the two things that decide whether
 * the keys are offered at all, and that creating the engines does not throw.
 */
@RunWith(AndroidJUnit4::class)
class DictationTest {

    @Test
    fun report_what_this_phone_can_do() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val onDevice = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        val dictation = Dictation(context)
        val speaking = Speaking(context)
        android.util.Log.i(
            "VOICE",
            "onDeviceRecognition=$onDevice dictationOffered=${dictation.available} " +
                "textToSpeech=${speaking.available}"
        )
        dictation.stop()
        speaking.release()
    }
}
