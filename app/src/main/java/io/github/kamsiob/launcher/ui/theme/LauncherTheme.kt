package io.github.kamsiob.launcher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration

/** Light or dark. Outlined is a separate switch that combines with either. */
enum class Look { LIGHT, DARK }

/** The three user text size steps, on top of the system font scale. */
enum class TextStep(val multiplier: Float) {
    ONE(1.0f),
    TWO(1.15f),
    THREE(1.3f),
}

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalOutlined = staticCompositionLocalOf { false }
val LocalTextStep = staticCompositionLocalOf { TextStep.ONE }

/**
 * Enlarged touch slop so a small drag from a tremor still lands as a tap.
 * The rest of the configuration, including the long press timeout the user
 * may have extended in accessibility settings, passes through untouched.
 */
private class ForgivingViewConfiguration(
    private val base: ViewConfiguration,
) : ViewConfiguration by base {
    override val touchSlop: Float get() = base.touchSlop * 2.5f
}

@Composable
fun LauncherTheme(
    look: Look,
    outlined: Boolean,
    textStep: TextStep,
    content: @Composable () -> Unit,
) {
    val palette = if (look == Look.DARK) DarkPalette else LightPalette
    val forgiving = ForgivingViewConfiguration(LocalViewConfiguration.current)
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalOutlined provides outlined,
        LocalTextStep provides textStep,
        LocalViewConfiguration provides forgiving,
        content = content,
    )
}

/** The screen background, applied by the root of every screen. */
@Composable
fun Modifier.screenBackground(): Modifier = background(LocalPalette.current.background)
