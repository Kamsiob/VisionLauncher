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
 * How long a key ignores a second press of itself.
 *
 * There is no single right answer, because the cost of the two mistakes is not
 * the same on every key, and one window for the whole app got this wrong.
 *
 * On a key that COMMITS something, firing twice is expensive and dropping a
 * press costs only a second press, so the window is generous.
 *
 * On a key a person legitimately presses in a row, the costs invert. A keypad
 * digit is additive, immediately visible, and has Erase beside it, so a stray
 * extra digit is cheap and self correcting. A dropped digit is expensive: the
 * number is quietly wrong and may be dialed without anyone noticing. Dialing
 * 555 at an ordinary pace was losing a press under the old single window, so
 * these keys get a window short enough to sit under a tremor's involuntary
 * repeat and well under a deliberate one.
 */
object TouchTiming {
    /** Keys that change something. A double fire would be the expensive mistake. */
    const val COMMIT_MS = 500L

    /** Keys a person presses in a row. A dropped press is the expensive mistake. */
    const val REPEAT_MS = 150L
}

/**
 * Every tappable surface in the app routes through this. Repeat taps inside
 * the window register once, so a tremor cannot fire an action twice. The
 * enlarged touch slop lives in the theme's ViewConfiguration; this handles
 * the time axis.
 *
 * A suppressed press still reports itself through [onSuppressed]. A key that
 * absorbs a press in silence is indistinguishable from a phone that has
 * stopped working, which for this audience is the worse failure.
 */
fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role = Role.Button,
    windowMs: Long = TouchTiming.COMMIT_MS,
    onSuppressed: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val lastTap = remember { mutableLongStateOf(0L) }
    clickable(enabled = enabled, onClickLabel = onClickLabel, role = role) {
        val now = SystemClock.uptimeMillis()
        if (now - lastTap.longValue >= windowMs) {
            lastTap.longValue = now
            onClick()
        } else {
            onSuppressed?.invoke()
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
