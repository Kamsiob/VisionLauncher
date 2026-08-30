package io.github.kamsiob.launcher.ui.seeing

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.seeing.Filter
import io.github.kamsiob.launcher.seeing.Magnifier
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.DarkPalette
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle

/**
 * Grid 10. The camera pointed at the physical world.
 *
 * Always dark, whatever theme the person chose, because this screen is held up
 * against a bright label and a paper colored interface beside it destroys the
 * contrast the magnifier exists to provide.
 *
 * Laid out twice. Upright the picture sits above the keys; sideways it sits
 * beside them, because on the short edge a stacked picture becomes a letterbox
 * and the keys fall off the bottom. The platform can decline a portrait
 * request, so this screen has to be right either way. See DECISIONS.md D47.
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
    // Measured from the window rather than read from Configuration. With
    // orientation in configChanges the activity is never recreated, and the
    // Configuration this composition sees can still describe the shape the
    // screen had when it opened.
    val window = LocalWindowInfo.current.containerSize
    val sideways = window.width > window.height
    val density = LocalDensity.current
    val shortEdgeDp = with(density) { minOf(window.width, window.height).toDp() }
    val longEdgeDp = with(density) { maxOf(window.width, window.height).toDp() }

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

    val viewfinder: @Composable (Modifier) -> Unit = { modifier ->
        Viewfinder(
            modifier = modifier,
            frozen = frozen,
            filter = filter,
            owner = owner,
            magnifier = magnifier,
            onCameraFailed = { failed = noCameraMessage },
        )
    }

    val controls: @Composable ColumnScope.() -> Unit = {
        Controls(
            frozen = frozen,
            filter = filter,
            zoomLabel = zoomLabel,
            torchOn = magnifier.torchOn,
            onZoom = { by ->
                magnifier.nudgeZoom(by)
                zoomLabel = (magnifier.zoom * 100).toInt()
            },
            onFilter = { filter = it },
            onTorch = {
                if (!magnifier.toggleTorch()) {
                    Haptics.reject(view)
                    failed = noTorchMessage
                } else {
                    failed = null
                }
            },
            onFreeze = {
                magnifier.freeze(
                    onFrozen = { frozen = it; failed = null },
                    onFailed = {
                        Haptics.reject(view)
                        failed = captureFailedMessage
                    },
                )
            },
            onLive = { frozen = null; failed = null },
            onRead = { frozen?.let(onRead) },
        )
    }

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

            if (sideways) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.gapColumn),
                ) {
                    viewfinder(Modifier.weight(1f).fillMaxHeight())
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Dimens.gap),
                        content = controls,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.gapColumn),
                ) {
                    // Never taller than most of the screen. It used to take
                    // whatever height the keys left over, and at 200 percent
                    // font scale the keys grew until the picture was a 150dp
                    // letterbox: the one thing here that must not shrink was
                    // the only thing shrinking.
                    viewfinder(
                        Modifier
                            .fillMaxWidth()
                            // Sized so the zoom keys, the colors key and Hold
                            // still all fit under it without scrolling at
                            // ordinary text sizes. A taller picture pushed Hold
                            // still off the bottom, which is the one key a
                            // person is reaching for while holding the phone
                            // over something.
                            .height(minOf(shortEdgeDp * 0.72f, longEdgeDp * 0.38f))
                    )
                    controls()
                }
            }

            NoteText(stringResource(R.string.reader_privacy))
        }
    }
}

/** The live preview, or the frame being held still. */
@Composable
private fun Viewfinder(
    modifier: Modifier,
    frozen: Bitmap?,
    filter: Filter,
    owner: androidx.lifecycle.LifecycleOwner,
    magnifier: Magnifier,
    onCameraFailed: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.radiusKey))
            .background(DarkPalette.card),
        contentAlignment = Alignment.Center,
    ) {
        if (frozen != null) {
            Image(
                bitmap = frozen.asImageBitmap(),
                contentDescription = stringResource(R.string.magnifier_frozen),
                // Crop, not fit. The frozen frame is the same shape as the
                // viewfinder now, and fitting it left grey bars either side on
                // any device where it is not, which made the picture appear to
                // jump to a different framing the moment it was held.
                contentScale = ContentScale.Crop,
                colorFilter = filter.matrix()?.let { ColorFilter.colorMatrix(it) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { preview ->
                        preview.scaleType = PreviewView.ScaleType.FILL_CENTER
                        // Touching the picture focuses there. Held close to a
                        // pill bottle the camera often focuses past it, and
                        // there was no way to tell it otherwise.
                        preview.setOnTouchListener { view, event ->
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                magnifier.focusAt(preview, event.x, event.y)
                                view.performClick()
                            }
                            true
                        }
                        magnifier.start(owner, preview) { onCameraFailed() }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    // The preview is a picture of the world, and no description
                    // of it can be written in advance. It stays silent so a
                    // screen reader moves straight to the keys, which are what
                    // can actually be operated.
                    .clearAndSetSemantics {},
            )
        }
    }
}

/** Zoom, colors, light, and holding a frame still. */
@Composable
private fun ColumnScope.Controls(
    frozen: Bitmap?,
    filter: Filter,
    zoomLabel: Int,
    torchOn: Boolean,
    onZoom: (Float) -> Unit,
    onFilter: (Filter) -> Unit,
    onTorch: () -> Unit,
    onFreeze: () -> Unit,
    onLive: () -> Unit,
    onRead: () -> Unit,
) {
    if (frozen == null) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            ApplianceKey(
                label = "−",
                contentDescription = stringResource(R.string.magnifier_zoom_out),
                onClick = { onZoom(-0.1f) },
                modifier = Modifier.weight(1f),
                fontSize = TypeScale.padKey,
                repeatable = true,
            )
            ApplianceKey(
                label = "+",
                contentDescription = stringResource(R.string.magnifier_zoom_in),
                onClick = { onZoom(0.1f) },
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

    FilterRow(current = filter, onPick = onFilter)

    // Equal heights. "Hold still" wraps to two lines where "Light" does not,
    // and without this the pair sits visibly uneven.
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.gap),
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        if (frozen == null) {
            ApplianceKey(
                label = stringResource(
                    if (torchOn) R.string.magnifier_light_off else R.string.magnifier_light
                ),
                icon = LineIcons.torch,
                onClick = onTorch,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
            )
            ApplianceKey(
                label = stringResource(R.string.magnifier_hold_still),
                icon = LineIcons.holdStill,
                onClick = onFreeze,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
                committing = true,
            )
        } else {
            ApplianceKey(
                label = stringResource(R.string.magnifier_live_again),
                onClick = onLive,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
            )
            ApplianceKey(
                label = stringResource(R.string.reader_read_this),
                icon = LineIcons.speaker,
                onClick = onRead,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                minHeight = Dimens.keySmall,
                fontSize = TypeScale.keyLabelSmall,
                style = KeyStyle.PRIMARY,
                committing = true,
            )
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
