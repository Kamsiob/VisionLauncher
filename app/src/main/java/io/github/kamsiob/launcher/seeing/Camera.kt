package io.github.kamsiob.launcher.seeing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.util.Rational
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import android.util.Size
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * The camera behind the magnifier.
 *
 * Freezing takes the frame the viewfinder is already showing rather than firing
 * the shutter. The first build used ImageCapture at maximum quality, and on a
 * real phone that meant several seconds of nothing happening after pressing
 * Hold still, sometimes ending in a failure, and a frozen picture in a
 * different shape from the preview because the shutter captures the whole
 * sensor while the viewfinder shows a square crop of it. Somebody framing a
 * pill bottle would press the key, wait, and get back a picture that was not
 * what they had lined up.
 *
 * Holding the analyzer's latest frame is instant, cannot fail once the preview
 * is running, and is by construction exactly what was on screen. The resolution
 * is lower than a photograph, which does not matter: recognition reads a
 * 1400 by 1000 frame in about a third of a second, and the person has already
 * zoomed in on the thing they want read.
 */
class Magnifier(private val context: Context) {

    private var provider: ProcessCameraProvider? = null
    private var control: CameraControl? = null
    private var info: CameraInfo? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val freezeRequested = AtomicBoolean(false)
    private var onFrozen: ((Bitmap) -> Unit)? = null

    var torchOn = false
        private set

    /** Where zoom sits, from 0 for the widest view to 1 for the closest. */
    var zoom = 0f
        private set

    val hasTorch: Boolean get() = info?.hasFlashUnit() == true

    fun start(
        owner: LifecycleOwner,
        view: PreviewView,
        onFailed: () -> Unit,
    ) {
        // Wait for the view to have a size. A PreviewView reports no view port
        // until it has been measured, and the camera provider is usually ready
        // first, so binding immediately bound with no view port at all and the
        // frame handed back covered the whole sensor rather than the square on
        // screen. Somebody framing a book cover got back everything above and
        // below it as well.
        if (view.width == 0 || view.height == 0) {
            view.doOnLayout { start(owner, view, onFailed) }
            return
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = runCatching { future.get() }.getOrNull()
            if (cameraProvider == null) {
                onFailed()
                return@addListener
            }
            provider = cameraProvider

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                // Enough for recognition without making every frame expensive.
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            )
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            analysis.setAnalyzer(analysisExecutor) { image ->
                // Frames are converted only when somebody asked for one.
                // Copying every frame would cost a full bitmap allocation
                // sixty times a second for a picture nobody is keeping.
                if (freezeRequested.compareAndSet(true, false)) {
                    val bitmap = runCatching {
                        // Cropped to the rectangle the view port asked for.
                        // toBitmap returns the whole buffer and ignores the crop
                        // rect, so setting a view port alone changed nothing
                        // about the picture that came back.
                        image.toBitmap()
                            .cropped(image.cropRect)
                            .rotated(image.imageInfo.rotationDegrees)
                    }.getOrNull()
                    if (bitmap != null) {
                        ContextCompat.getMainExecutor(context).execute {
                            onFrozen?.invoke(bitmap)
                        }
                    }
                }
                image.close()
            }

            // The view port is what makes the frozen frame match the viewfinder.
            // Without it the preview shows a square crop and the analyzer hands
            // over the whole sensor, so what was framed and what was frozen are
            // two different pictures.
            // The view port is what makes the frozen frame match the
            // viewfinder. PreviewView supplies one once it is laid out; where
            // it does not, one is built from the view's own shape rather than
            // binding without and silently going back to the whole sensor.
            val viewPort = view.viewPort ?: ViewPort.Builder(
                Rational(view.width, view.height),
                preview.targetRotation,
            ).setScaleType(ViewPort.FILL_CENTER).build()
            val group = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(analysis)
                .setViewPort(viewPort)
                .build()

            val bound = runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    group,
                )
            }.getOrNull()
            if (bound == null) {
                onFailed()
                return@addListener
            }
            control = bound.cameraControl
            info = bound.cameraInfo
            // The magnifier exists to make small things big, so it opens partway
            // in rather than at the widest view nobody came here for.
            setZoom(0.35f)
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        runCatching { if (torchOn) control?.enableTorch(false) }
        torchOn = false
        onFrozen = null
        freezeRequested.set(false)
        runCatching { provider?.unbindAll() }
        control = null
        info = null
    }

    fun release() {
        stop()
        runCatching { analysisExecutor.shutdown() }
    }

    /** Returns true when the torch actually changed, so the key can say so. */
    fun toggleTorch(): Boolean {
        val camera = control ?: return false
        if (info?.hasFlashUnit() != true) return false
        val next = !torchOn
        return runCatching {
            camera.enableTorch(next)
            torchOn = next
            true
        }.getOrDefault(false)
    }

    fun nudgeZoom(by: Float) = setZoom(zoom + by)

    private fun setZoom(to: Float) {
        val clamped = max(0f, min(1f, to))
        zoom = clamped
        runCatching { control?.setLinearZoom(clamped) }
    }

    /**
     * Focuses where somebody touched the picture.
     *
     * A phone held close to a pill bottle focuses on whatever the camera
     * decided was interesting, which at that distance is often the background.
     * Without a way to say "this bit", the magnifier hands back a blurred frame
     * and the reader finds no words in it.
     */
    fun focusAt(view: PreviewView, x: Float, y: Float) {
        val camera = control ?: return
        runCatching {
            val point = view.meteringPointFactory.createPoint(x, y)
            camera.startFocusAndMetering(
                FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .disableAutoCancel()
                    .build()
            )
        }
    }

    /**
     * Freezes a frame so a shaking hand can rest.
     *
     * Held in memory rather than written to storage. A magnified pill bottle is
     * not a photograph anybody wants keeping, and saving it would put a picture
     * of somebody's medication in their gallery.
     *
     * Returns immediately; the frame arrives on the main thread within a frame
     * or two. If the camera is not running, [onFailed] is called rather than
     * leaving the key looking pressed forever.
     */
    fun freeze(onFrozen: (Bitmap) -> Unit, onFailed: () -> Unit) {
        if (control == null) return onFailed()
        this.onFrozen = onFrozen
        freezeRequested.set(true)
        // If no frame arrives, say so. The analyzer runs continuously while the
        // preview is up so this should never fire, but a key that quietly does
        // nothing is the failure this app cannot afford, and "should never" is
        // not a guarantee on somebody else's phone.
        ContextCompat.getMainExecutor(context).execute {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (freezeRequested.compareAndSet(true, false)) onFailed()
            }, FREEZE_TIMEOUT_MS)
        }
    }

    private companion object {
        const val FREEZE_TIMEOUT_MS = 1500L
    }
}

/** Takes the part of the frame the view port asked for, and nothing else. */
private fun Bitmap.cropped(rect: android.graphics.Rect): Bitmap {
    val left = rect.left.coerceIn(0, width)
    val top = rect.top.coerceIn(0, height)
    val w = rect.width().coerceIn(1, width - left)
    val h = rect.height().coerceIn(1, height - top)
    if (left == 0 && top == 0 && w == width && h == height) return this
    return Bitmap.createBitmap(this, left, top, w, h)
}

/** The sensor does not rotate with the phone, so the frame arrives sideways. */
private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
