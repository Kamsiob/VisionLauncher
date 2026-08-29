package io.github.kamsiob.launcher.support

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Reading text out loud, using whichever engine the phone already has.
 *
 * Deliberately not a bundled voice. The person's own TalkBack voice is the one
 * they have already tuned for speed and pitch, and it is the voice they are
 * used to; substituting a different one would make the app sound like a
 * stranger. Where no engine is installed the key is not offered.
 */
class Speaking(context: Context) {

    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var onDone: (() -> Unit)? = null

    init {
        engine = runCatching {
            TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    engine?.language = Locale.getDefault()
                    engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onDone(utteranceId: String?) {
                            onDone?.invoke()
                        }

                        override fun onStart(utteranceId: String?) = Unit

                        @Deprecated("Required by the platform base class")
                        override fun onError(utteranceId: String?) {
                            onDone?.invoke()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            onDone?.invoke()
                        }
                    })
                    // A speak call made before the engine finished starting is
                    // dropped by the platform without any error, so the key
                    // would look pressed and stay silent.
                    pending?.let { text -> pending = null; speak(text) }
                }
            }
        }.getOrNull()
    }

    val available: Boolean get() = engine != null

    fun speak(text: String, whenFinished: () -> Unit = {}) {
        onDone = whenFinished
        val tts = engine ?: return whenFinished()
        if (!ready) {
            pending = text
            return
        }
        runCatching {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE)
        }.onFailure { whenFinished() }
    }

    fun stop() {
        pending = null
        runCatching { engine?.stop() }
    }

    fun release() {
        pending = null
        onDone = null
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
    }

    private companion object {
        const val UTTERANCE = "message"
    }
}
