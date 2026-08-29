package io.github.kamsiob.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.github.kamsiob.launcher.R

/**
 * Three faces, bundled in res/font, never downloaded.
 *
 * Young Serif renders the clock and the onboarding headings only. Atkinson
 * Hyperlegible Next is everything else. Atkinson Hyperlegible Mono carries
 * timestamps, the status pill, and metadata rows.
 */
@OptIn(ExperimentalTextApi::class)
val AtkinsonNext = FontFamily(
    Font(
        R.font.atkinson_hyperlegible_next,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.atkinson_hyperlegible_next,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.atkinson_hyperlegible_next,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.atkinson_hyperlegible_next,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

@OptIn(ExperimentalTextApi::class)
val AtkinsonMono = FontFamily(
    Font(
        R.font.atkinson_hyperlegible_mono,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.atkinson_hyperlegible_mono,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

val YoungSerif = FontFamily(Font(R.font.young_serif))

/**
 * The type scale from the grid, in sp. The user's text size step multiplies
 * these on top of the system font scale, never instead of it. Line heights are
 * in sp through TextUnit so they scale with the text.
 */
object TypeScale {
    const val clock = 94
    const val title = 34
    const val tileLabel = 28
    const val keyLabel = 26
    const val keyLabelSmall = 24
    const val body = 24
    const val rowMeta = 16
    const val statusPill = 19
    const val dayline = 28
    const val dateline = 19
    const val note = 17
    const val lampCount = 18
    const val lampSay = 23
    const val undo = 21
    const val promptBar = 22
    const val doneChip = 21
    const val sect = 21
    const val sublabel = 18
    const val dialed = 34
    const val padKey = 50
    const val h2 = 36
    const val avatarInitial = 26
    const val appIconInitial = 26
    const val nowTag = 15
    const val bigmsg = 34
    const val from = 22
    /** Grid 11 sets the recognized text at 30px, below the 34px message body. */
    const val readerText = 30
}

/** Multiplies a base sp size by the user's chosen text step. */
@Composable
@ReadOnlyComposable
fun stepSp(base: Int): TextUnit = (base * LocalTextStep.current.multiplier).sp

@Composable
@ReadOnlyComposable
fun bodyStyle(
    size: Int = TypeScale.body,
    weight: FontWeight = FontWeight.Medium,
    lineHeightFactor: Float = 1.45f,
): TextStyle = TextStyle(
    fontFamily = AtkinsonNext,
    fontWeight = weight,
    fontSize = stepSp(size),
    lineHeight = stepSp((size * lineHeightFactor).toInt()),
    // Direction follows the content rather than the layout. In an Arabic
    // layout an untranslated English sentence would otherwise be laid out
    // right to left and its full stop would appear at the visual left, which
    // is how ".appearing" showed up on the attention lamp. It also keeps a
    // Latin app name upright inside an Arabic sentence once translations land.
    textDirection = TextDirection.Content,
)

@Composable
@ReadOnlyComposable
fun monoStyle(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    lineHeightFactor: Float = 1.5f,
): TextStyle = TextStyle(
    fontFamily = AtkinsonMono,
    fontWeight = weight,
    fontSize = stepSp(size),
    lineHeight = stepSp((size * lineHeightFactor).toInt()),
    textDirection = TextDirection.Content,
)

@Composable
@ReadOnlyComposable
fun serifStyle(size: Int, lineHeightFactor: Float = 1.15f): TextStyle = TextStyle(
    fontFamily = YoungSerif,
    fontWeight = FontWeight.Normal,
    fontSize = stepSp(size),
    lineHeight = stepSp((size * lineHeightFactor).toInt()),
    textDirection = TextDirection.Content,
)
