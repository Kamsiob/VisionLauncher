package io.github.kamsiob.launcher.messages

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Speech to text for replies, on this device only.
 *
 * The screen tells people "Your voice is understood on this phone. Nothing is
 * sent anywhere." That sentence is only allowed to appear when it is true, so
 * this wrapper refuses to fall back to the cloud recognizer. Where on-device
 * recognition is not available the speak key is not offered at all and the
 * phrases and the keyboard carry the screen. A promise about privacy that
 * quietly degrades is worse than a feature that is missing.
 */
class Dictation(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    /** Whether the speak key can be offered at all. */
    val available: Boolean
        get() = Build.VERSION.SDK_INT >= 31 &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
                .getOrDefault(false)

    fun start(
        onResult: (String) -> Unit,
        onNothingHeard: () -> Unit,
        onLevel: (Float) -> Unit = {},
    ) {
        // The API level is checked here as well as in `available`, because the
        // on-device recognizer only exists from 31 and this app runs from 29.
        // Relying on the property alone left the call itself unguarded as far
        // as any reader, or any tool, could tell.
        if (Build.VERSION.SDK_INT < 31 || !available) {
            onNothingHeard()
            return
        }
        stop()
        val created = runCatching {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        }.getOrNull()
        if (created == null) {
            onNothingHeard()
            return
        }
        recognizer = created
        created.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isEmpty()) onNothingHeard() else onResult(text)
            }

            override fun onError(error: Int) = onNothingHeard()
            override fun onRmsChanged(rmsdB: Float) = onLevel(rmsdB)
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // Older speakers pause mid sentence. The platform defaults cut them
            // off partway through and send half a reply.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2500L,
            )
        }
        runCatching { created.startListening(intent) }.onFailure { onNothingHeard() }
    }

    fun stop() {
        recognizer?.let { r ->
            runCatching { r.stopListening() }
            runCatching { r.destroy() }
        }
        recognizer = null
    }
}
