package io.github.kamsiob.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kamsiob.launcher.seeing.Reading
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
/**
 * Proves the bundled recognizer actually recognizes, on the device, with no
 * network at all. The trained data ships inside the APK, and the app holds no
 * INTERNET permission, so if this passes then the privacy sentence on the
 * reader screen is true rather than merely intended. See DECISIONS.md D44.
 */
@RunWith(AndroidJUnit4::class)
class ReadingTest {

    private fun labelBitmap(vararg lines: String): Bitmap {
        val bitmap = Bitmap.createBitmap(1000, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 72f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, 60f, 140f + index * 110f, paint)
        }
        return bitmap
    }

    private fun readBlocking(bitmap: Bitmap): String? =
        Reading.read(ApplicationProvider.getApplicationContext(), bitmap)

    @Test
    fun reads_the_words_on_a_medicine_label() {
        val text = readBlocking(
            labelBitmap("AMOXICILLIN 500 mg", "Take one capsule", "three times daily")
        )
        assertTrue("nothing was recognized", text != null)
        val flat = text!!.replace("\n", " ")
        assertTrue("missing the drug name, got: $flat", flat.contains("AMOXICILLIN"))
        assertTrue("missing the dose, got: $flat", flat.contains("500"))
        assertTrue("missing the instruction, got: $flat", flat.contains("capsule", true))
    }

    @Test
    fun a_blank_frame_reports_nothing_rather_than_empty_text() {
        val blank = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        assertTrue("a blank frame should recognize nothing", readBlocking(blank) == null)
    }

    @Test
    fun speaking_form_puts_a_pause_between_lines() {
        val spoken = Reading.forSpeaking("AMOXICILLIN 500 mg\nTake one capsule\nthree times daily")
        assertTrue(
            "lines must be separated for a voice, got: $spoken",
            spoken == "AMOXICILLIN 500 mg. Take one capsule. three times daily",
        )
    }
}
