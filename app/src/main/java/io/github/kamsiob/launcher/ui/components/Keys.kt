package io.github.kamsiob.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kamsiob.launcher.support.Haptics
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
    iconSize: Dp = 32.dp,
    iconTint: Color? = null,
    sublabel: String? = null,
    contentDescription: String? = null,
    committing: Boolean = false,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    val content = keyContentColor(style)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .keySurface(style = style)
            .debouncedClickable(enabled = enabled, onClickLabel = null) {
                if (committing) Haptics.confirm(view) else Haptics.tap(view)
                onClick()
            }
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.semantics(mergeDescendants = true) {}
                }
            ),
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
            Text(
                text = label,
                style = bodyStyle(size = fontSize, weight = FontWeight.Bold, lineHeightFactor = 1.2f),
                color = content,
                textAlign = TextAlign.Center,
            )
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .keySurface()
            .debouncedClickable(enabled = enabled) {
                Haptics.tap(view)
                onClick()
            }
            .defaultMinSize(minHeight = Dimens.tile)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.semantics(mergeDescendants = true) {}
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
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
        Text(
            text = label,
            style = bodyStyle(size = TypeScale.tileLabel, weight = FontWeight.Bold, lineHeightFactor = 1.15f),
            color = palette.text,
            textAlign = TextAlign.Center,
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .keySurface()
            .debouncedClickable(enabled = enabled) {
                if (committing) Haptics.confirm(view) else Haptics.tap(view)
                onClick()
            }
            .defaultMinSize(minHeight = Dimens.rowKey)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.semantics(mergeDescendants = true) {}
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            avatarInitial != null -> Avatar(initial = avatarInitial, color = avatarColor)
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(Dimens.rowIcon),
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
