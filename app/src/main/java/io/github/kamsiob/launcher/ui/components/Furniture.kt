package io.github.kamsiob.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.support.debouncedClickable
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalOutlined
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.Tokens
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.monoStyle
import io.github.kamsiob.launcher.ui.theme.serifStyle

/**
 * The screen frame: paper background, grid padding, a column with the grid's
 * 16dp gap. The top bar with Home and optional Back arrives through [topBar].
 */
@Composable
fun ScreenFrame(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    topBar: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(
                start = Dimens.screenSide,
                end = Dimens.screenSide,
                top = Dimens.screenTop,
                bottom = Dimens.screenBottom,
            )
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = Arrangement.spacedBy(Dimens.gapColumn),
    ) {
        if (topBar != null) topBar()
        content()
    }
}

/** The Home and optional Back keys that sit at the top of inner screens. */
@Composable
fun TopBar(
    onHome: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.gap),
    ) {
        ApplianceKey(
            label = stringResource(R.string.key_home),
            onClick = onHome,
            modifier = Modifier.weight(1f),
            minHeight = Dimens.homeKey,
            fontSize = TypeScale.keyLabelSmall,
            icon = LineIcons.home,
            iconSize = Dimens.homeKeyIcon,
            contentDescription = stringResource(R.string.a11y_home_key),
        )
        if (onBack != null) {
            ApplianceKey(
                label = stringResource(R.string.key_back),
                onClick = onBack,
                modifier = Modifier.weight(1f),
                minHeight = Dimens.homeKey,
                fontSize = TypeScale.keyLabelSmall,
                contentDescription = stringResource(R.string.a11y_back_key),
            )
        }
    }
}

/** Screen title: 34sp, weight 800, accent color. Announced as a heading. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = bodyStyle(size = TypeScale.title, weight = FontWeight.ExtraBold, lineHeightFactor = 1.15f),
        color = LocalPalette.current.accent,
        modifier = modifier.semantics { heading() },
    )
}

/** The serif heading used by onboarding and the threshold screen. */
@Composable
fun SerifHeading(text: String, size: Int = TypeScale.h2, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = serifStyle(size = size),
        color = LocalPalette.current.accent,
        modifier = modifier.semantics { heading() },
    )
}

/** Body copy at 24sp for explanation screens. */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = bodyStyle(),
        color = LocalPalette.current.text,
        modifier = modifier,
    )
}

/**
 * The masthead. Navy, edge to edge, carrying the serif clock, the day part
 * line with its mark, and the mono date line. TalkBack reads it as one idea.
 */
@Composable
fun Masthead(
    clockText: String,
    dayPartText: String,
    dateText: String,
    isEvening: Boolean,
) {
    val palette = LocalPalette.current
    val markDescription = stringResource(
        if (isEvening) R.string.a11y_moon_mark else R.string.a11y_sun_mark
    )
    val merged = stringResource(R.string.a11y_masthead, clockText, dayPartText, dateText)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.masthead)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 34.dp, bottom = 26.dp)
            .semantics(mergeDescendants = true) { contentDescription = merged },
    ) {
        Text(
            text = clockText,
            style = serifStyle(size = TypeScale.clock, lineHeightFactor = 1f),
            color = palette.mastheadText,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Icon(
                imageVector = if (isEvening) LineIcons.moon else LineIcons.sun,
                contentDescription = markDescription,
                tint = palette.mastheadText,
                modifier = Modifier.size(Dimens.daylineIcon),
            )
            Text(
                text = dayPartText,
                style = bodyStyle(size = TypeScale.dayline, weight = FontWeight.Medium, lineHeightFactor = 1.2f),
                color = palette.mastheadText,
            )
        }
        Text(
            text = dateText,
            style = monoStyle(size = TypeScale.dateline, lineHeightFactor = 1.4f),
            color = palette.dateLineText,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * The status pill. A complete green sentence when nothing is wrong. In the
 * Outlined theme it gains a 2dp green border, as the grid specifies.
 */
@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val outlined = LocalOutlined.current
    val shape = RoundedCornerShape(Dimens.radiusPill)
    Row(
        modifier = modifier
            .clip(shape)
            .background(palette.statusBg)
            .then(if (outlined) Modifier.border(Dimens.statusBorder, palette.green, shape) else Modifier)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.statusDot)
                .background(palette.green, RoundedCornerShape(Dimens.radiusPill)),
        )
        Text(
            text = text,
            style = monoStyle(size = TypeScale.statusPill, lineHeightFactor = 1.35f),
            color = palette.statusText,
        )
    }
}

/** One item in the attention queue. */
data class LampItem(
    val id: String,
    val sentence: String,
    val icon: ImageVector,
    val repairLabel: String? = null,
    val onRepair: (() -> Unit)? = null,
)

/**
 * The attention lamp: one banner, many items, each a plain sentence with a
 * repair key where a one tap fix genuinely exists. The app's only caution
 * voice. Never red.
 */
@Composable
fun AttentionLamp(items: List<LampItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    val palette = LocalPalette.current
    val outlined = LocalOutlined.current
    val shape = RoundedCornerShape(Dimens.radiusLamp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (outlined) {
                    Modifier
                        .clip(shape)
                        .background(palette.lamp)
                        .border(Dimens.outlinedBorder, palette.outline, shape)
                } else {
                    Modifier
                        .shadow(3.dp, shape, ambientColor = palette.shadow, spotColor = palette.shadow)
                        .background(palette.lamp, shape)
                }
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (items.size > 1) {
            Text(
                text = pluralStringResource(R.plurals.attention_count, items.size, items.size),
                style = monoStyle(size = TypeScale.lampCount, weight = FontWeight.Bold),
                color = palette.lampText,
            )
        }
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(thickness = 2.dp, color = palette.lampText.copy(alpha = 0.18f))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = palette.lampText,
                        modifier = Modifier.size(Dimens.lampIcon),
                    )
                    Text(
                        text = item.sentence,
                        style = bodyStyle(size = TypeScale.lampSay, weight = FontWeight.Bold, lineHeightFactor = 1.3f),
                        color = palette.lampText,
                    )
                }
                if (item.repairLabel != null && item.onRepair != null) {
                    ApplianceKey(
                        label = item.repairLabel,
                        onClick = item.onRepair,
                        style = KeyStyle.PRIMARY,
                        minHeight = Dimens.keySmall,
                        fontSize = TypeScale.keyLabelSmall,
                        committing = true,
                    )
                }
            }
        }
    }
}

/**
 * The lamp colored undo strip: the message on the left, the underlined act on
 * the right. Used when something was taken off and can come back.
 */
@Composable
fun UndoStrip(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val outlined = LocalOutlined.current
    val view = LocalView.current
    val shape = RoundedCornerShape(Dimens.radiusUndo)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (outlined) {
                    Modifier
                        .clip(shape)
                        .background(palette.lamp)
                        .border(Dimens.outlinedBorder, palette.outline, shape)
                } else {
                    Modifier
                        .shadow(3.dp, shape, ambientColor = palette.shadow, spotColor = palette.shadow)
                        .background(palette.lamp, shape)
                }
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            style = bodyStyle(size = TypeScale.undo, weight = FontWeight.Bold, lineHeightFactor = 1.25f),
            color = palette.lampText,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = actionLabel,
            style = bodyStyle(size = TypeScale.undo, weight = FontWeight.Bold, lineHeightFactor = 1.25f)
                .copy(textDecoration = TextDecoration.Underline),
            color = if (palette.isDark) Tokens.navyDeep else palette.accent,
            modifier = Modifier
                .padding(start = 12.dp)
                .defaultMinSize(minHeight = 48.dp)
                .debouncedClickable {
                    Haptics.confirm(view)
                    onAction()
                }
                .padding(8.dp),
        )
    }
}

/**
 * The prompt bar for arranging mode: instruction on the left, the permanent
 * Done chip on the right. The exit is always one visible tap.
 */
@Composable
fun PromptBar(
    text: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val palette = LocalPalette.current
    val view = LocalView.current
    val shape = RoundedCornerShape(Dimens.radiusPromptBar)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (palette.isDark) palette.masthead else palette.accent, shape)
            .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            .then(if (dimmed) Modifier.alpha(0.38f) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = bodyStyle(size = TypeScale.promptBar, weight = FontWeight.Bold, lineHeightFactor = 1.3f),
            color = Tokens.cream,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .background(Tokens.cream, RoundedCornerShape(Dimens.radiusDoneChip))
                .debouncedClickable {
                    Haptics.confirm(view)
                    onDone()
                }
                .defaultMinSize(minWidth = 72.dp, minHeight = 48.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.key_done),
                style = bodyStyle(size = TypeScale.doneChip, weight = FontWeight.ExtraBold, lineHeightFactor = 1f),
                color = Tokens.navy,
            )
        }
    }
}
