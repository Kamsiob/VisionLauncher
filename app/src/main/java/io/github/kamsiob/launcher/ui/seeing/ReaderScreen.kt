package io.github.kamsiob.launcher.ui.seeing

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.seeing.Filter
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.DarkPalette
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.monoStyle

/**
 * Grid 11. What the frozen frame said, large, and read aloud.
 *
 * Dark for the same reason the magnifier is: this screen follows one held up
 * against a bright label, and switching to a paper background between the two
 * would flash the eyes it exists to spare.
 */
@Composable
fun ReaderScreen(
    frame: Bitmap?,
    text: String?,
    working: Boolean,
    speaking: Boolean,
    canSpeak: Boolean,
    filter: Filter,
    onFilter: (Filter) -> Unit,
    onRead: () -> Unit,
    onStop: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
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
            TopBar(onHome = onHome, onBack = onBack)

            if (frame != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.readerFrame)
                        .clip(RoundedCornerShape(Dimens.radiusKey))
                        .background(DarkPalette.card),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = filter.matrix()?.let { ColorFilter.colorMatrix(it) },
                        modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
                    )
                }
            }

            when {
                working -> Text(
                    text = stringResource(R.string.reader_working),
                    style = bodyStyle(),
                    color = DarkPalette.text,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )

                speaking -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.statusDot)
                            .clip(CircleShape)
                            .background(DarkPalette.green),
                    )
                    Text(
                        text = stringResource(R.string.reader_reading_now),
                        style = monoStyle(size = TypeScale.statusPill),
                        color = DarkPalette.text,
                    )
                }
            }

            if (text != null) {
                // The recognized words are the point of the screen, so they get
                // the height that is left and scroll inside it rather than
                // pushing the keys off the bottom on a long label.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = text,
                        style = bodyStyle(size = TypeScale.readerText, lineHeightFactor = 1.4f),
                        color = DarkPalette.text,
                    )
                }
            } else if (!working) {
                Text(
                    text = stringResource(R.string.reader_nothing_found),
                    style = bodyStyle(),
                    color = DarkPalette.text,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { liveRegion = LiveRegionMode.Assertive },
                )
            } else {
                Box(Modifier.weight(1f))
            }

            if (text != null) FilterRow(current = filter, onPick = onFilter)

            if (text != null && canSpeak) {
                ApplianceKey(
                    label = stringResource(
                        if (speaking) R.string.reader_stop else R.string.reader_again
                    ),
                    onClick = if (speaking) onStop else onRead,
                    minHeight = Dimens.keySmall,
                    style = if (speaking) KeyStyle.NORMAL else KeyStyle.PRIMARY,
                )
            }
            if (text != null && !canSpeak) {
                NoteText(stringResource(R.string.reader_no_voice))
            }

            NoteText(stringResource(R.string.reader_privacy))
        }
    }
}
