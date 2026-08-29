package io.github.kamsiob.launcher.ui.seeing

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.seeing.Filter
import io.github.kamsiob.launcher.seeing.Magnifier
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.DarkPalette
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp

/**
 * Grid 10. The camera pointed at the physical world.
 *
 * Always dark, whatever theme the person chose, because this screen is held up
 * against a bright label and a paper colored interface beside it destroys the
 * contrast the magnifier exists to provide.
 */
@Composable
fun MagnifierScreen(
    onRead: (Bitmap) -> Unit,
    onGrantCamera: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current
    val hasCamera = remember {
        context.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
    val magnifier = remember { Magnifier(context) }
    var frozen by remember { mutableStateOf<Bitmap?>(null) }
    var filter by remember { mutableStateOf(Filter.NORMAL) }
    var failed by remember { mutableStateOf<String?>(null) }
    var zoomLabel by remember { mutableStateOf(35) }
    // Read in the composition rather than from the context inside a click
    // handler, so they follow a language or configuration change instead of
    // holding whatever was current when the screen first opened.
    val noTorchMessage = stringResource(R.string.magnifier_no_torch)
    val captureFailedMessage = stringResource(R.string.magnifier_capture_failed)
    val noCameraMessage = stringResource(R.string.magnifier_no_camera)

    DisposableEffect(Unit) { onDispose { magnifier.stop() } }

    CompositionLocalProvider(LocalPalette provides DarkPalette) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkPalette.background)
                .statusBarsPadding()
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(
                    start = Dimens.screenSide,
                    end = Dimens.screenSide,
                    top = Dimens.screenTop,
                    bottom = Dimens.screenBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(Dimens.gapColumn),
        ) {
            TopBar(onHome = onHome)

            if (!hasCamera) {
                Text(
                    text = stringResource(R.string.magnifier_no_permission),
                    style = bodyStyle(),
                    color = DarkPalette.text,
                )
                ApplianceKey(
                    label = stringResource(R.string.magnifier_allow_camera),
                    onClick = onGrantCamera,
                    style = KeyStyle.PRIMARY,
                )
                return@CompositionLocalProvider
            }

            failed?.let { message ->
                Text(
                    text = message,
                    style = bodyStyle(),
                    color = DarkPalette.text,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }

            // The viewfinder takes whatever height the keys do not, so the
            // magnified world is always the largest thing on the screen.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(Dimens.radiusKey))
                    .background(DarkPalette.card),
                contentAlignment = Alignment.Center,
            ) {
                val held = frozen
                if (held != null) {
                    Image(
                        bitmap = held.asImageBitmap(),
                        contentDescription = stringResource(R.string.magnifier_frozen),
                        contentScale = ContentScale.Fit,
                        colorFilter = filter.matrix()?.let { ColorFilter.colorMatrix(it) },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { preview ->
                                preview.scaleType = PreviewView.ScaleType.FILL_CENTER
                                magnifier.start(owner, preview) { failed = noCameraMessage }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            // The preview is a picture of the world, and no
                            // description of it can be written in advance. It
                            // stays silent so a screen reader moves straight to
                            // the keys, which are what can actually be operated.
                            .clearAndSetSemantics {},
                    )
                }
            }

            if (frozen == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
                    ApplianceKey(
                        label = "−",
                        contentDescription = stringResource(R.string.magnifier_zoom_out),
                        onClick = {
                            magnifier.nudgeZoom(-0.1f)
                            zoomLabel = (magnifier.zoom * 100).toInt()
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = TypeScale.padKey,
                        repeatable = true,
                    )
                    ApplianceKey(
                        label = "+",
                        contentDescription = stringResource(R.string.magnifier_zoom_in),
                        onClick = {
                            magnifier.nudgeZoom(0.1f)
                            zoomLabel = (magnifier.zoom * 100).toInt()
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = TypeScale.padKey,
                        repeatable = true,
                    )
                }
                Text(
                    text = stringResource(R.string.a11y_zoom_level, zoomLabel),
                    style = bodyStyle(size = TypeScale.note),
                    color = DarkPalette.note,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            FilterRow(current = filter, onPick = { filter = it })

            // Equal heights. "Hold still" wraps to two lines where "Light" does
            // not, and without this the pair sits visibly uneven.
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.gap),
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                if (frozen == null) {
                    ApplianceKey(
                        label = stringResource(
                            if (magnifier.torchOn) R.string.magnifier_light_off
                            else R.string.magnifier_light
                        ),
                        icon = LineIcons.torch,
                        onClick = {
                            if (!magnifier.toggleTorch()) {
                                Haptics.reject(view)
                                failed = noTorchMessage
                            } else {
                                failed = null
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        minHeight = Dimens.keySmall,
                        fontSize = TypeScale.keyLabelSmall,
                    )
                    ApplianceKey(
                        label = stringResource(R.string.magnifier_hold_still),
                        icon = LineIcons.holdStill,
                        onClick = {
                            magnifier.freeze(
                                onFrozen = { frozen = it; failed = null },
                                onFailed = {
                                    Haptics.reject(view)
                                    failed = captureFailedMessage
                                },
                            )
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        minHeight = Dimens.keySmall,
                        fontSize = TypeScale.keyLabelSmall,
                        committing = true,
                    )
                } else {
                    ApplianceKey(
                        label = stringResource(R.string.magnifier_live_again),
                        onClick = { frozen = null; failed = null },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        minHeight = Dimens.keySmall,
                        fontSize = TypeScale.keyLabelSmall,
                    )
                    ApplianceKey(
                        label = stringResource(R.string.reader_read_this),
                        icon = LineIcons.speaker,
                        onClick = { frozen?.let(onRead) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        minHeight = Dimens.keySmall,
                        fontSize = TypeScale.keyLabelSmall,
                        style = KeyStyle.PRIMARY,
                        committing = true,
                    )
                }
            }

            NoteText(stringResource(R.string.reader_privacy))
        }
    }
}

/**
 * One full width key that names the colors in use and advances to the next set.
 *
 * Five keys in a row do not fit at any text size this app supports, and the
 * scrolling row they became put "Yellow on black" past the right edge, where
 * somebody who needs it most would never find it. A key that cycles keeps every
 * option reachable, keeps the current one readable without opening anything,
 * and stays one press. See DECISIONS.md D45.
 */
@Composable
fun FilterRow(current: Filter, onPick: (Filter) -> Unit) {
    val name = stringResource(current.labelRes)
    ApplianceKey(
        label = stringResource(R.string.magnifier_colors_now, name),
        // Read as a state rather than as an instruction, and announced again
        // when it changes, because the change is the whole feedback.
        contentDescription = stringResource(R.string.a11y_colors_now, name),
        onClick = { onPick(current.next()) },
        minHeight = Dimens.keySmall,
        fontSize = TypeScale.keyLabelSmall,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
