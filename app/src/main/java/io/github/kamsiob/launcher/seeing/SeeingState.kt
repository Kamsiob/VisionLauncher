package io.github.kamsiob.launcher.seeing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.kamsiob.launcher.support.Speaking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The state behind the magnifier, the reader, and photos.
 *
 * Held on the activity so a frozen frame survives navigating from the
 * magnifier to the reader and back, and so the voice is built once rather than
 * on each visit.
 */
class SeeingState(
    private val context: Context,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {

    private val speech = Speaking(context)
    val photoStore = PhotoStore(context)

    val canSpeak: Boolean get() = speech.available

    var frozen by mutableStateOf<Bitmap?>(null)
        private set

    var recognized by mutableStateOf<String?>(null)
        private set

    var working by mutableStateOf(false)
        private set

    var speaking by mutableStateOf(false)
        private set

    var filter by mutableStateOf(Filter.NORMAL)

    /**
     * Called when the magnifier hands a held frame to the reader.
     *
     * Recognition runs off the main thread. Tesseract on a full camera frame
     * takes a second or two, and doing that on the main thread would freeze the
     * launcher itself for that whole time.
     */
    fun readFrame(frame: Bitmap) {
        frozen = frame
        recognized = null
        working = true
        scope.launch {
            val text = withContext(Dispatchers.Default) { Reading.read(context, frame) }
            working = false
            recognized = text
            if (text != null) speakRecognized()
        }
    }

    fun speakRecognized() {
        val text = recognized ?: return
        if (!speech.available) return
        speaking = true
        speech.speak(Reading.forSpeaking(text)) { speaking = false }
    }

    fun say(text: String) {
        if (!speech.available) return
        speaking = true
        speech.speak(text) { speaking = false }
    }

    fun stopSpeaking() {
        speech.stop()
        speaking = false
    }

    /**
     * Clears the held frame and its words.
     *
     * The bitmap is a full resolution camera frame, several megabytes of it,
     * and holding one after the screen is gone is the difference between this
     * feature costing nothing at rest and the launcher being killed for memory
     * later in the day.
     */
    fun clearFrame() {
        stopSpeaking()
        frozen = null
        recognized = null
        working = false
    }

    /**
     * Decodes a photo at a size that fits a phone screen rather than at its
     * full resolution. A modern camera photo decoded whole is around fifty
     * megabytes as a bitmap, and three of those in a row would end the process.
     */
    suspend fun loadPhoto(photo: Photo): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, photo.uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.isMutableRequired = false
                    val longest = maxOf(info.size.width, info.size.height)
                    if (longest > MAX_EDGE) {
                        val factor = longest / MAX_EDGE
                        decoder.setTargetSampleSize(maxOf(1, factor))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(
                    context.contentResolver, photo.uri
                )
            }
        }.getOrNull()?.asImageBitmap()
    }

    fun release() {
        speech.release()
        // The native engine holds its language model outside the Java heap, so
        // nothing reclaims it when this object is collected.
        Reading.release()
        frozen = null
    }

    private companion object {
        const val MAX_EDGE = 2048
    }
}
