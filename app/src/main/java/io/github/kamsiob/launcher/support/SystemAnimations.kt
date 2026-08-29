package io.github.kamsiob.launcher.support

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Motion is minimal and functional, and the one deliberate animation is the
 * slow tile trade during arranging. A person who turned animations off in
 * accessibility settings meant it, so that setting is respected everywhere
 * rather than only where it is convenient.
 */
object SystemAnimations {
    fun removed(context: Context): Boolean {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        return scale == 0f
    }
}

/** The tile trade duration, or zero when the system says no animations. */
@Composable
fun tradeDurationMillis(): Int {
    val context = LocalContext.current
    return remember(context) { if (SystemAnimations.removed(context)) 0 else 420 }
}
