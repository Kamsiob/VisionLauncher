package io.github.kamsiob.launcher.seeing

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One picture, with whatever the phone actually knows about where it came from. */
data class Photo(
    val uri: Uri,
    val takenAt: Long,
    /** The album folder, which is how "from Michael" is known at all. */
    val bucket: String?,
)

/**
 * Reads the device media store. One photo at a time, no albums, no editing.
 *
 * Only images, only from the shared store, and only what a person would call a
 * photo. Screenshots and downloaded stickers are excluded, because a photo
 * screen for somebody's grandchildren should not be four fifths receipts.
 */
class PhotoStore(private val context: Context) {

    fun hasPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return context.checkSelfPermission(permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun load(limit: Int = 500): List<Photo> {
        if (!hasPermission()) return emptyList()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val columns = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val photos = mutableListOf<Photo>()
        runCatching {
            context.contentResolver.query(
                collection,
                columns,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idAt = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val takenAt = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val addedAt = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val bucketAt = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext() && photos.size < limit) {
                    val bucket = if (bucketAt >= 0) cursor.getString(bucketAt) else null
                    if (bucket != null && bucket.lowercase(Locale.ROOT) in SKIPPED) continue
                    val id = cursor.getLong(idAt)
                    // DATE_TAKEN is milliseconds and can be absent; DATE_ADDED
                    // is seconds and always exists. Mixing the units silently
                    // dates half the library to 1970.
                    val taken = takenAt.takeIf { it >= 0 }
                        ?.let { cursor.getLong(it) }
                        ?.takeIf { it > 0 }
                        ?: (addedAt.takeIf { it >= 0 }?.let { cursor.getLong(it) * 1000L } ?: 0L)
                    photos += Photo(
                        uri = Uri.withAppendedPath(collection, id.toString()),
                        takenAt = taken,
                        bucket = bucket,
                    )
                }
            }
        }
        return photos
    }

    private companion object {
        val SKIPPED = setOf("screenshots", "screen recordings", "stickers", "icons")
    }
}

/**
 * The caption under a photo: which one of how many, who it came from where
 * that is knowable, and when.
 *
 * A folder name is the only source for who sent a picture, and only messaging
 * apps write one worth reading. Where there is no such name the caption simply
 * does not claim one, rather than guessing.
 */
fun photoCaption(
    context: Context,
    photo: Photo,
    index: Int,
    total: Int,
): String {
    val position = context.getString(
        io.github.kamsiob.launcher.R.string.photos_position, index + 1, total
    )
    val from = photo.bucket?.let { senderFromBucket(it) }
    val when_ = if (photo.takenAt > 0) friendlyDay(photo.takenAt) else null
    return listOfNotNull(
        position,
        from?.let { context.getString(io.github.kamsiob.launcher.R.string.photos_from, it) },
        when_,
    ).joinToString(", ")
}

/**
 * A folder like "WhatsApp Images" says which app, not which person, so it is
 * reported as the app it is. Claiming a name the phone does not know would be
 * a caption that lies with confidence.
 */
private fun senderFromBucket(bucket: String): String? {
    val trimmed = bucket.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.lowercase(Locale.ROOT) in setOf("camera", "dcim", "pictures", "download")) {
        return null
    }
    return trimmed
}

/** Today, Yesterday, a weekday inside the last week, then a date. */
private fun friendlyDay(at: Long): String {
    val context = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = at }
    val days = ((context.timeInMillis - at) / (24L * 60 * 60 * 1000)).toInt()
    val locale = Locale.getDefault()
    return when {
        sameDay(then, context) -> SimpleDateFormat("h:mm a", locale).format(Date(at))
        days in 1..6 -> SimpleDateFormat("EEEE", locale).format(Date(at))
        else -> SimpleDateFormat(
            android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMd"), locale
        ).format(Date(at))
    }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
