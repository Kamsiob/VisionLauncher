package io.github.kamsiob.launcher.seeing

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.util.Locale

/**
 * Optical character recognition on a frozen frame, on this phone only.
 *
 * Tesseract rather than ML Kit. ML Kit recognizes better, and it was tried
 * first, but it hard references Google's telemetry uploader and will not
 * initialize without it, and that uploader is the only thing that put an
 * INTERNET permission into the merged manifest. The app's central promise is
 * that it holds no such permission and that anybody can check. See
 * DECISIONS.md D44 for the evidence and the reasoning.
 *
 * The trained data ships inside the APK, so nothing is ever downloaded.
 */
object Reading {

    /** The languages Stage 4 localizes to, each with its data bundled. */
    private val SUPPORTED = mapOf(
        "en" to "eng",
        "es" to "spa",
        "zh" to "chi_sim",
        "ar" to "ara",
    )

    @Volatile private var api: TessBaseAPI? = null
    @Volatile private var loadedFor: String? = null

    /**
     * Copies the trained data out of the assets once, because Tesseract reads
     * it from a real directory rather than from the APK. Returns the parent
     * that Tesseract expects, which is the folder containing `tessdata`.
     */
    private fun dataDir(context: Context, language: String): File? = runCatching {
        val root = File(context.filesDir, "tesseract")
        val tessdata = File(root, "tessdata").apply { mkdirs() }
        val file = File(tessdata, "$language.traineddata")
        if (!file.exists() || file.length() == 0L) {
            context.assets.open("tessdata/$language.traineddata").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        root
    }.getOrNull()

    /** The bundled language closest to how the phone is set, English otherwise. */
    private fun languageFor(context: Context): String {
        val tag = context.resources.configuration.locales[0].language.lowercase(Locale.ROOT)
        return SUPPORTED[tag] ?: "eng"
    }

    private fun engine(context: Context): TessBaseAPI? {
        val language = languageFor(context)
        api?.let { if (loadedFor == language) return it }
        // The phone's language changed under a cached engine, so the old one is
        // reading with the wrong alphabet and has to go.
        api?.let { runCatching { it.recycle() } }
        api = null
        loadedFor = null
        val root = dataDir(context, language) ?: return null
        val created = runCatching {
            TessBaseAPI().also { tess ->
                if (!tess.init(root.absolutePath, language)) {
                    tess.recycle()
                    return null
                }
                tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            }
        }.getOrNull() ?: return null
        api = created
        loadedFor = language
        return created
    }

    /**
     * Reads the words in a frame and hands back one block of text.
     *
     * Blocking, and expected to be called off the main thread. Recognition on
     * a full camera frame takes a second or two on a mid range phone.
     */
    fun read(context: Context, bitmap: Bitmap): String? {
        val tess = engine(context) ?: return null
        // Two passes. The first assumes a page and finds the lines on it, which
        // is right for a label held square on. The second assumes text scattered
        // anywhere in the frame, which is what a curved bottle or a sign at an
        // angle actually looks like. The second costs about a third of a second
        // and only runs when the first came back with nothing, so the common
        // case pays nothing for it and the awkward case is rescued instead of
        // being told there are no words.
        val modes = listOf(
            TessBaseAPI.PageSegMode.PSM_AUTO,
            TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT,
        )
        for (mode in modes) {
            val text = runCatching {
                tess.pageSegMode = mode
                tess.setImage(bitmap)
                val found = tess.utF8Text?.trim().orEmpty()
                tess.clear()
                found
            }.getOrDefault("")
            // A single stray character is noise, not a reading. Two passes over
            // a blank wall will both find something eventually.
            if (text.count { it.isLetterOrDigit() } >= 3) return text
        }
        return null
    }

    /**
     * The same text, punctuated for a voice rather than for an eye.
     *
     * A label reads "AMOXICILLIN 500 mg" on one line and "Take one capsule" on
     * the next. Spoken with no pause between them it becomes one run-on
     * sentence. A line break is a breath.
     */
    fun forSpeaking(text: String): String =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(". ") { line -> line.trimEnd('.', ',', ';', ':') }

    /** Frees the native engine, which holds its language model in native memory. */
    fun release() {
        api?.let { runCatching { it.recycle() } }
        api = null
        loadedFor = null
    }
}
