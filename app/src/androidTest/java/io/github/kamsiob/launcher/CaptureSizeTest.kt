package io.github.kamsiob.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kamsiob.launcher.seeing.Reading
import org.junit.Test
import org.junit.runner.RunWith

/**
 * How long recognition takes at the size the camera actually hands over.
 *
 * The reader worked in every test and failed on a real phone. The tests fed it
 * a 1.4 megapixel bitmap; ImageCapture on a Pixel 8 at maximize-quality hands
 * over about twelve. This measures both, because a recognizer that takes half a
 * minute is indistinguishable from one that does not work.
 */
@RunWith(AndroidJUnit4::class)
class CaptureSizeTest {

    private fun load(name: String): Bitmap {
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets
            .open(name).use { it.readBytes() }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun time(label: String, bitmap: Bitmap) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val start = System.currentTimeMillis()
        val text = Reading.read(context, bitmap)
        val took = System.currentTimeMillis() - start
        val flat = text.orEmpty().replace("\n", " | ")
        android.util.Log.i(
            "CAPSIZE",
            "$label ${bitmap.width}x${bitmap.height} took ${took}ms -> ${flat.take(90)}"
        )
    }

    @Test
    fun measure_recognition_at_camera_resolution() {
        time("small", load("label-photo.jpg"))
        time("full", load("label-photo-full.jpg"))
    }
}
