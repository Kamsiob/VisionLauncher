package io.github.kamsiob.launcher.support

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Every tappable surface in the app routes through this. Repeat taps inside
 * the window register once, so a tremor cannot fire an action twice. The
 * enlarged touch slop lives in the theme's ViewConfiguration; this handles
 * the time axis.
 */
fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role = Role.Button,
    windowMs: Long = 500L,
    onClick: () -> Unit,
): Modifier = composed {
    val lastTap = remember { mutableLongStateOf(0L) }
    clickable(enabled = enabled, onClickLabel = onClickLabel, role = role) {
        val now = SystemClock.uptimeMillis()
        if (now - lastTap.longValue >= windowMs) {
            lastTap.longValue = now
            onClick()
        }
    }
}

/**
 * Haptics. Hearing loss travels with vision loss, so sound alone never
 * confirms anything. An ordinary key press ticks; a committed action, one
 * that changes something, gets the distinct confirmation effect.
 */
object Haptics {
    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun confirm(view: View) {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun reject(view: View) {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
