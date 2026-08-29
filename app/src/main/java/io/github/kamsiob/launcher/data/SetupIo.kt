package io.github.kamsiob.launcher.data

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Reading and writing the setup file and the printable sheet through the
 * system file picker.
 *
 * The picker is used rather than a path the app chooses, so the file lands
 * wherever the person keeps their own files and the app never needs storage
 * permission to write it. Every call reports whether it worked, because these
 * three actions return from another app's screen with nothing else to show
 * what happened.
 */
object SetupIo {

    fun writeText(context: Context, uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(text.toByteArray())
            true
        } ?: false
    }.getOrDefault(false)

    fun readText(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            // Read by hand rather than with readNBytes, which only exists from
            // API 33 and would throw NoSuchMethodError on the oldest phones this
            // app supports. Capped, because the picker will happily hand over a
            // video file and reading it whole into a string would end the
            // process.
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            while (buffer.size() < MAX_BYTES) {
                val read = stream.read(chunk)
                if (read <= 0) break
                buffer.write(chunk, 0, minOf(read, MAX_BYTES - buffer.size()))
            }
            buffer.toString(Charsets.UTF_8.name())
        }
    }.getOrNull()

    /** A setup file over a megabyte is not a setup file. */
    private const val MAX_BYTES = 1024 * 1024
}
