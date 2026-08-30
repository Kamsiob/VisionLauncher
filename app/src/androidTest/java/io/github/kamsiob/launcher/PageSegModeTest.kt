package io.github.kamsiob.launcher

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.googlecode.tesseract.android.TessBaseAPI
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Which page segmentation mode actually reads a photograph of a label.
 *
 * The first build used PSM_AUTO_OSD, which asks Tesseract to detect the
 * orientation and script of the page before reading it. That detection needs
 * osd.traineddata, which this app does not ship, and the synthetic bitmap in
 * ReadingTest was clean enough to come back anyway. A real photograph is not,
 * which is why the reader failed on a phone and passed in a test.
 *
 * This runs the same photograph through each mode and prints what came back,
 * so the choice is made on evidence rather than on a guess.
 */
@RunWith(AndroidJUnit4::class)
class PageSegModeTest {

    @Test
    fun report_what_each_page_segmentation_mode_reads() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets
            .open("label-photo.jpg").use { it.readBytes() }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        requireNotNull(bitmap) { "the test image did not decode" }

        val root = File(context.filesDir, "tesseract")
        File(root, "tessdata").mkdirs()
        val data = File(root, "tessdata/eng.traineddata")
        if (!data.exists() || data.length() == 0L) {
            context.assets.open("tessdata/eng.traineddata").use { input ->
                data.outputStream().use { input.copyTo(it) }
            }
        }

        val modes = listOf(
            "PSM_AUTO_OSD" to TessBaseAPI.PageSegMode.PSM_AUTO_OSD,
            "PSM_AUTO" to TessBaseAPI.PageSegMode.PSM_AUTO,
            "PSM_SINGLE_BLOCK" to TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK,
            "PSM_SPARSE_TEXT" to TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT,
        )
        for ((name, mode) in modes) {
            val tess = TessBaseAPI()
            val ok = tess.init(root.absolutePath, "eng")
            if (!ok) {
                android.util.Log.e("PSM", "$name: init FAILED")
                continue
            }
            tess.pageSegMode = mode
            tess.setImage(bitmap)
            val text = runCatching { tess.utF8Text.orEmpty() }.getOrElse { "THREW: $it" }
            tess.recycle()
            val flat = text.replace("\n", " | ").trim()
            val hit = listOf("AMOXICILLIN", "500", "capsule").count { flat.contains(it, true) }
            android.util.Log.i("PSM", "$name -> $hit/3 keywords: ${flat.take(160)}")
        }
    }
}
