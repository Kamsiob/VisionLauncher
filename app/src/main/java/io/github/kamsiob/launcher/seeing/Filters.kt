package io.github.kamsiob.launcher.seeing

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ColorMatrix
import io.github.kamsiob.launcher.R

/**
 * The contrast filters from MASTER_SPEC 5.7, applied to the live preview and
 * to a frozen frame alike.
 *
 * These are color matrices rather than shaders so the same value can drive a
 * Compose paint filter and a bitmap redraw without two implementations that
 * could drift apart. Yellow on black is last because it is the one people
 * reach for after the others have failed, not the one to lead with.
 */
enum class Filter(val id: String, @param:StringRes val labelRes: Int) {
    NORMAL("normal", R.string.filter_normal),
    GRAY("gray", R.string.filter_gray),
    HIGH_CONTRAST("high", R.string.filter_high_contrast),
    INVERTED("inverted", R.string.filter_inverted),
    YELLOW_ON_BLACK("yellow", R.string.filter_yellow_on_black);

    /** Null means draw the frame untouched, which is cheaper than an identity matrix. */
    fun matrix(): ColorMatrix? = when (this) {
        NORMAL -> null
        GRAY -> ColorMatrix().apply { setToSaturation(0f) }
        // Desaturate first, then push the midtones apart. Raising contrast on
        // a color image drives saturated pixels to clip long before the text
        // separates from the paper.
        HIGH_CONTRAST -> ColorMatrix().apply {
            setToSaturation(0f)
            timesAssign(ColorMatrix(contrastValues(2.6f)))
        }
        INVERTED -> ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        // Luminance to a single channel, inverted, then tinted. Black paper
        // with yellow ink is the highest contrast pairing that does not use
        // pure white, which is what makes it bearable for hours.
        YELLOW_ON_BLACK -> ColorMatrix(
            floatArrayOf(
                -0.30f, -0.59f, -0.11f, 0f, 255f,
                -0.26f, -0.51f, -0.10f, 0f, 220f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    /** The next one round the ring, so one key can reach all five. */
    fun next(): Filter = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromId(id: String?): Filter = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

/** A contrast matrix pivoted on mid gray, so raising it does not also brighten. */
private fun contrastValues(amount: Float): FloatArray {
    val shift = (1f - amount) * 127.5f
    return floatArrayOf(
        amount, 0f, 0f, 0f, shift,
        0f, amount, 0f, 0f, shift,
        0f, 0f, amount, 0f, shift,
        0f, 0f, 0f, 1f, 0f,
    )
}
