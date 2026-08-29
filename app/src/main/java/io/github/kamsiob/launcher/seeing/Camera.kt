package io.github.kamsiob.launcher.seeing

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlin.math.max
import kotlin.math.min

/**
 * The camera behind the magnifier.
 *
 * Wrapped rather than used directly because everything here can fail on a real
 * phone in a way the screen has to survive: no camera, no torch, a camera held
 * by another app, a capture that never returns. Each entry point reports what
 * happened instead of throwing into a launcher that has to stay open.
 */
class Magnifier(private val context: Context) {

    private var provider: ProcessCameraProvider? = null
    private var control: CameraControl? = null
    private var info: CameraInfo? = null
    private var capture: ImageCapture? = null

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
            val imageCapture = ImageCapture.Builder()
                // The frozen frame is read by OCR and shown large, so latency
                // matters less than not being a blur.
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            capture = imageCapture
            val bound = runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
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
        runCatching { provider?.unbindAll() }
        control = null
        info = null
        capture = null
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
     * Freezes a frame so a shaking hand can rest.
     *
     * Captured to memory rather than to a file. A magnified pill bottle is not
     * a photograph anybody wants keeping, and writing it to storage would put
     * a picture of somebody's medication in their gallery.
     */
    fun freeze(onFrozen: (Bitmap) -> Unit, onFailed: () -> Unit) {
        val imageCapture = capture ?: return onFailed()
        runCatching {
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = runCatching {
                            image.toBitmap().rotated(image.imageInfo.rotationDegrees)
                        }.getOrNull()
                        image.close()
                        if (bitmap == null) onFailed() else onFrozen(bitmap)
                    }

                    override fun onError(exception: ImageCaptureException) = onFailed()
                },
            )
        }.onFailure { onFailed() }
    }
}

/** The sensor does not rotate with the phone, so the frame arrives sideways. */
private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
