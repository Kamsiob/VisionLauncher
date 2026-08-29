package io.github.kamsiob.launcher.ui.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import io.github.kamsiob.launcher.ui.theme.Tokens
import kotlin.math.absoluteValue

/**
 * Calling never takes the dialer role. With CALL_PHONE granted the call is
 * placed directly; without it the phone's own dialer opens with the number
 * filled in, which always works.
 */
fun placeCall(context: Context, number: String) {
    val uri = Uri.parse("tel:" + Uri.encode(number))
    val direct = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED
    val intent = Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
}

/** The avatar palette from the grid: navy plus the earthy companions. */
private val avatarColors = listOf(
    Tokens.navy,
    Tokens.green,
    Color(0xFF5B4A3A),
    Color(0xFF4E7A9B),
    Color(0xFF8A5A3A),
    Color(0xFFB08D3A),
)

/** A stable color per person, so Sarah is always the same color. */
fun avatarColorFor(name: String): Color =
    avatarColors[name.hashCode().absoluteValue % avatarColors.size]

fun initialOf(name: String): String =
    name.trim().firstOrNull()?.uppercase() ?: "?"
