package io.github.kamsiob.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kamsiob.launcher.support.Haptics
import io.github.kamsiob.launcher.support.TouchTiming
import io.github.kamsiob.launcher.support.debouncedClickable
import io.github.kamsiob.launcher.ui.theme.AtkinsonMono
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LocalOutlined
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.Tokens
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import io.github.kamsiob.launcher.ui.theme.monoStyle
import io.github.kamsiob.launcher.ui.theme.stepSp

enum class KeyStyle { NORMAL, PRIMARY, EMERGENCY }

/**
 * One separation device per element: a key gets a soft shadow, or in the
 * Outlined theme a 3dp border, never both. Every key in the app draws its
 * face through this.
 */
@Composable
fun Modifier.keySurface(
    shape: Shape = RoundedCornerShape(Dimens.radiusKey),
    style: KeyStyle = KeyStyle.NORMAL,
): Modifier {
    val palette = LocalPalette.current
    val outlined = LocalOutlined.current
    val face = when (style) {
        KeyStyle.NORMAL -> palette.card
        KeyStyle.PRIMARY -> palette.primaryKey
        KeyStyle.EMERGENCY -> palette.red
    }
    return if (outlined) {
        val borderColor = when (style) {
            KeyStyle.NORMAL -> palette.outline
            KeyStyle.PRIMARY -> if (palette.isDark) palette.outline else Tokens.navyDeep
            KeyStyle.EMERGENCY -> if (palette.isDark) palette.outline else Tokens.navyDeep
        }
        this
            .clip(shape)
            .background(face)
            .border(Dimens.outlinedBorder, borderColor, shape)
    } else {
        this
            .shadow(
                elevation = 3.dp,
                shape = shape,
                ambientColor = palette.shadow,
                spotColor = palette.shadow,
            )
            .background(face, shape)
    }
}

@Composable
fun keyContentColor(style: KeyStyle): Color {
    val palette = LocalPalette.current
    return when (style) {
        KeyStyle.NORMAL -> palette.text
        KeyStyle.PRIMARY -> palette.onPrimaryKey
        KeyStyle.EMERGENCY -> palette.onRed
    }
}

/**
 * The appliance key: a large, obvious, honest button. Centered label,
 * optional leading icon, optional sublabel underneath.
 */
@Composable
fun ApplianceKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: KeyStyle = KeyStyle.NORMAL,
    minHeight: Dp = Dimens.keyMin,
    fontSize: Int = TypeScale.keyLabel,
    icon: ImageVector? = null,
    // Sized to the key rather than fixed. One value used to serve the 72dp,
    // 88dp and 130dp keys alike, which is how two different sizes in the grid
    // both collapsed to one.
    iconSize: Dp = when {
        minHeight <= Dimens.keySmall -> Dimens.keyIconSmall
        minHeight >= Dimens.bigKey -> Dimens.keyIconBig
        else -> Dimens.keyIcon
    },
    iconTint: Color? = null,
    sublabel: String? = null,
    contentDescription: String? = null,
    committing: Boolean = false,
    // True for a key a person legitimately presses in a row, such as a keypad
    // digit or a plus key. See TouchTiming.
    repeatable: Boolean = false,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    val content = keyContentColor(style)
    val spoken = contentDescription
        ?: listOfNotNull(label, sublabel).joinToString(". ")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .keySurface(style = style)
            .debouncedClickable(
                enabled = enabled,
                onClickLabel = null,
                windowMs = if (repeatable) TouchTiming.REPEAT_MS else TouchTiming.COMMIT_MS,
                // A press the window swallows still ticks, so the key never
                // feels dead under the finger.
                onSuppressed = { Haptics.tap(view) },
            ) {
                if (committing) Haptics.confirm(view) else Haptics.tap(view)
                onClick()
            }
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .clearAndSetSemantics {
                this.contentDescription = spoken
                role = Role.Button
                if (!enabled) disabled()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.gap, Alignment.CenterHorizontally),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: content,
                    modifier = Modifier.size(iconSize),
                )
            }
            // A phrase may wrap across lines, but a single word never splits.
            // "Erase" on a 96dp keypad key at 200 percent has nowhere to wrap
            // to, so the type steps down instead of breaking into "Eras e".
            val style = bodyStyle(size = fontSize, weight = FontWeight.Bold, lineHeightFactor = 1.2f)
                .copy(color = content, textAlign = TextAlign.Center)
            if (label.contains(' ')) {
                Text(text = label, style = style)
            } else {
                BasicText(
                    text = label,
                    style = style,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = stepSp(TypeScale.rowMeta),
                        maxFontSize = stepSp(fontSize),
                        stepSize = 1.sp,
                    ),
                )
            }
        }
        if (sublabel != null) {
            Text(
                text = sublabel,
                style = bodyStyle(size = TypeScale.sublabel, weight = FontWeight.Medium, lineHeightFactor = 1.25f),
                color = content,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/**
 * The home tile: 128dp, icon over label. Built in features pass a line icon;
 * third party apps pass their real icon bitmap.
 */
@Composable
fun Tile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    appIcon: ImageBitmap? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    val palette = LocalPalette.current
    val spoken = contentDescription ?: label
    Column(
        modifier = modifier
            .fillMaxWidth()
            .keySurface()
            .debouncedClickable(
                enabled = enabled,
                onSuppressed = { Haptics.tap(view) },
            ) {
                Haptics.tap(view)
                onClick()
            }
            .defaultMinSize(minHeight = Dimens.tile)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .clearAndSetSemantics {
                this.contentDescription = spoken
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        // Both glyph kinds sit in one slot of the same height. A line icon is
        // larger than an app's own bitmap on purpose, which is the cue that
        // separates a built in feature from an installed app, and without the
        // shared slot that difference would also make the two tiles different
        // heights and rag the row.
        Box(
            modifier = Modifier.height(Dimens.tileIconSlot),
            contentAlignment = Alignment.Center,
        ) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(Dimens.tileIcon),
                )
                appIcon != null -> Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Dimens.appIcon)
                        .clip(RoundedCornerShape(Dimens.appIconRadius)),
                )
            }
        }
        // The same rule the appliance key follows. A one word label like
        // "Messages" gets a single line and steps down to keep it whole; a two
        // word app name may wrap between its words. Allowing two lines and
        // letting the type shrink does not work, because a mid word break
        // "fits" and the shrinking stops there.
        val labelStyle = bodyStyle(size = TypeScale.tileLabel, weight = FontWeight.Bold, lineHeightFactor = 1.15f)
            .copy(color = palette.text, textAlign = TextAlign.Center)
        BasicText(
            text = label,
            style = labelStyle,
            maxLines = if (label.contains(' ')) 2 else 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = stepSp(TypeScale.rowMeta),
                maxFontSize = stepSp(TypeScale.tileLabel),
                stepSize = 1.sp,
            ),
        )
    }
}

/**
 * The row key: left aligned, a leading element, a label, and an optional mono
 * metadata line. 94dp floor.
 */
@Composable
fun RowKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    icon: ImageVector? = null,
    appIcon: ImageBitmap? = null,
    avatarInitial: String? = null,
    avatarColor: Color? = null,
    contentDescription: String? = null,
    committing: Boolean = false,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    val palette = LocalPalette.current
    val spoken = contentDescription ?: listOfNotNull(label, meta).joinToString(". ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .keySurface()
            .debouncedClickable(
                enabled = enabled,
                onSuppressed = { Haptics.tap(view) },
            ) {
                if (committing) Haptics.confirm(view) else Haptics.tap(view)
                onClick()
            }
            .defaultMinSize(minHeight = Dimens.rowKey)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .clearAndSetSemantics {
                this.contentDescription = spoken
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            avatarInitial != null -> Avatar(initial = avatarInitial, color = avatarColor)
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accent,
                // The grid draws a larger glyph on rows that carry only a
                // label than on rows with a metadata line under it.
                modifier = Modifier.size(
                    if (meta == null) Dimens.rowIcon else Dimens.rowIconWithMeta
                ),
            )
            appIcon != null -> Image(
                bitmap = appIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(Dimens.appIcon)
                    .clip(RoundedCornerShape(Dimens.appIconRadius)),
            )
        }
        Column {
            Text(
                text = label,
                style = bodyStyle(size = TypeScale.keyLabel, weight = FontWeight.Bold, lineHeightFactor = 1.2f),
                color = palette.text,
            )
            if (meta != null) {
                Text(
                    text = meta,
                    style = monoStyle(size = TypeScale.rowMeta, lineHeightFactor = 1.35f),
                    color = palette.textSoft,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
fun Avatar(initial: String, color: Color? = null, size: Dp = Dimens.avatar) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .size(size)
            .background(color ?: palette.accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = bodyStyle(size = TypeScale.avatarInitial, weight = FontWeight.ExtraBold, lineHeightFactor = 1f),
            color = if (LocalPalette.current.isDark) Tokens.darkBg else Tokens.cream,
        )
    }
}

/** The mono metadata text style used by notes at the bottom of screens. */
@Composable
fun NoteText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = monoStyle(size = TypeScale.note, lineHeightFactor = 1.5f),
        color = LocalPalette.current.note,
        modifier = modifier,
    )
}

/** Section label inside a screen, 21sp bold, left aligned. */
@Composable
fun SectLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = bodyStyle(size = TypeScale.sect, weight = FontWeight.Bold, lineHeightFactor = 1.2f),
        color = LocalPalette.current.text,
        modifier = modifier,
    )
}
