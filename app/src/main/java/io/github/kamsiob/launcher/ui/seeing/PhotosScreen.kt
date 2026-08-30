package io.github.kamsiob.launcher.ui.seeing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.seeing.Photo
import io.github.kamsiob.launcher.seeing.photoCaption
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.StatusPill
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LocalPalette

/**
 * Grid 12. One photo, two keys, no pinch.
 *
 * Deliberately not a grid of thumbnails. A wall of small squares is the thing
 * this audience cannot use, and choosing from it requires hitting a target a
 * third the size of the keys everywhere else in the app.
 */
@Composable
fun PhotosScreen(
    photos: List<Photo>,
    hasPermission: Boolean,
    loadFrame: suspend (Photo) -> ImageBitmap?,
    onGrantPhotos: () -> Unit,
    onOpenGallery: () -> Unit,
    onSpeakCaption: (String) -> Unit,
    canSpeak: Boolean,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    var index by remember(photos) { mutableStateOf(0) }
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }

    val current = photos.getOrNull(index)
    LaunchedEffect(current) {
        // Cleared first so a slow decode never shows the previous picture under
        // the next one's caption, which would say the wrong thing about it.
        frame = null
        frame = current?.let { loadFrame(it) }
    }

    val caption = current?.let { photoCaption(context, it, index, photos.size) }

    ScreenFrame(scrollable = false) {
        TopBar(onHome = onHome)

        // A title only when there is no picture. With a photo on screen the
        // picture is the page and a heading would take room from it, which is
        // how grid 12 draws it. Without one the screen was unnamed: nothing
        // said where you were, on screen or to a screen reader.
        if (!hasPermission || photos.isEmpty()) {
            ScreenTitle(stringResource(R.string.photos_title))
        }

        when {
            !hasPermission -> {
                NoteText(stringResource(R.string.photos_no_permission))
                ApplianceKey(
                    label = stringResource(R.string.photos_allow),
                    onClick = onGrantPhotos,
                    style = KeyStyle.PRIMARY,
                )
            }

            photos.isEmpty() -> {
                NoteText(stringResource(R.string.photos_none))
                ApplianceKey(
                    label = stringResource(R.string.photos_open_gallery),
                    onClick = onOpenGallery,
                    minHeight = Dimens.keySmall,
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimens.radiusKey))
                        .background(palette.card),
                    contentAlignment = Alignment.Center,
                ) {
                    frame?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            // Silent. The caption below says everything that can
                            // truthfully be said about the picture, and carrying
                            // it here too made a screen reader announce the same
                            // sentence twice for every photo.
                            modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
                        )
                    }
                }

                if (caption != null) {
                    StatusPill(text = caption, announce = true)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                    ApplianceKey(
                        label = stringResource(R.string.photos_back),
                        onClick = { if (index > 0) index-- },
                        modifier = Modifier.weight(1f),
                        enabled = index > 0,
                    )
                    ApplianceKey(
                        label = stringResource(R.string.photos_next),
                        onClick = { if (index < photos.lastIndex) index++ },
                        modifier = Modifier.weight(1f),
                        enabled = index < photos.lastIndex,
                    )
                }

                if (canSpeak && caption != null) {
                    ApplianceKey(
                        label = stringResource(R.string.photos_read_caption),
                        onClick = { onSpeakCaption(caption) },
                        minHeight = Dimens.keySmall,
                    )
                }
            }
        }
    }
}
