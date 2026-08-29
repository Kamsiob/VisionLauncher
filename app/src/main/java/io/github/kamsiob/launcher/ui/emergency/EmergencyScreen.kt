package io.github.kamsiob.launcher.ui.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.EmergencyContact
import io.github.kamsiob.launcher.ui.call.placeCall
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens

/**
 * Grid 05. Call 911 hands off to the phone's own dialer with the number
 * ready; the app never reimplements emergency calling. The alert key calls
 * the chosen person and texts them a location. The honesty line stays.
 */
@Composable
fun EmergencyScreen(
    emergencyContact: EmergencyContact?,
    onSetUpPerson: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val emergencyNumber = stringResource(R.string.emergency_number)
    // The alert key describes what this phone can actually do right now. The
    // permissions behind the text are requested when the helper chooses the
    // person, but they can be refused there or revoked later, and a key that
    // still promised a text would be exactly the silent failure D7 exists to
    // prevent.
    val permittedToText = remember(context) { granted(context, Manifest.permission.SEND_SMS) }
    val phoneCanText = remember(context) { smsManager(context) != null }
    val canLocate = remember(context) {
        val permitted = granted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        // Permission alone is not enough. With location switched off there is
        // no fix to attach, and the key would be promising a place it cannot
        // name.
        permitted && manager?.isLocationEnabled == true
    }
    val alertSubtitle = stringResource(
        when {
            permittedToText && phoneCanText && canLocate -> R.string.emergency_alert_person_sub
            permittedToText && phoneCanText -> R.string.emergency_alert_person_sub_text_only
            // Refused and cannot are different facts and deserve different words.
            !phoneCanText -> R.string.emergency_alert_person_sub_no_texting
            else -> R.string.emergency_alert_person_sub_call_only
        }
    )
    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.emergency_title))
        ApplianceKey(
            label = stringResource(R.string.emergency_call_number, emergencyNumber),
            sublabel = stringResource(R.string.emergency_call_number_sub, emergencyNumber),
            onClick = { dialEmergency(context, emergencyNumber) },
            style = KeyStyle.EMERGENCY,
            minHeight = Dimens.bigKey,
            committing = true,
        )
        if (emergencyContact != null) {
            ApplianceKey(
                label = stringResource(R.string.emergency_alert_person, emergencyContact.name),
                sublabel = alertSubtitle,
                onClick = { alertPerson(context, emergencyContact) },
                style = KeyStyle.PRIMARY,
                minHeight = Dimens.bigKey,
                committing = true,
            )
        } else {
            ApplianceKey(
                label = stringResource(R.string.emergency_no_person),
                sublabel = stringResource(R.string.emergency_no_person_sub),
                onClick = onSetUpPerson,
                minHeight = Dimens.bigKey,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        NoteText(stringResource(R.string.emergency_honesty))
    }
}

/**
 * The dialer opens with the emergency number entered and waits for the person to
 * press call.
 *
 * ACTION_DIAL rather than ACTION_CALL is deliberate and must stay that way. A
 * stray tap is exactly the failure this audience is prone to, and it is the
 * reason the Call screen's red key opens this screen instead of dialing. An
 * accidental emergency call is a worse outcome than one extra press, and the
 * key says the number is ready rather than claiming to place the call.
 *
 * The number arrives from a non translatable resource so the label and the dial
 * can never disagree, which they could when one was a string and the other a
 * Kotlin literal.
 */
private fun dialEmergency(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/**
 * Calls the chosen person and sends them a text with a location link when
 * location is available. The text goes out before the call intent fires so
 * the call screen taking over cannot swallow it.
 */
private fun alertPerson(context: Context, person: EmergencyContact) {
    val location = lastKnownLocation(context)
    val body = if (location != null) {
        context.getString(
            R.string.emergency_sms_body,
            "https://maps.google.com/?q=${location.first},${location.second}",
        )
    } else {
        context.getString(R.string.emergency_sms_body_no_location)
    }
    val sms = smsManager(context)
    if (sms != null && granted(context, Manifest.permission.SEND_SMS)) {
        runCatching {
            sms.sendMultipartTextMessage(
                person.number, null, sms.divideMessage(body), null, null,
            )
        }
    }
    placeCall(context, person.number)
}

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/**
 * The SMS manager, on every version this app supports.
 *
 * `getSystemService(SmsManager::class.java)` only exists from API 31 and returns
 * null below it, and minSdk is 29. The previous code called it unconditionally
 * inside a bare runCatching, so on Android 10 and 11 the emergency text threw a
 * null pointer, the failure was swallowed, and the call went out with no message
 * and nothing said. Returns null when this phone genuinely cannot send, which is
 * what the key's subtitle reads to decide what to promise.
 */
private fun smsManager(context: Context): SmsManager? = runCatching {
    if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(SmsManager::class.java)
    } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
    }
}.getOrNull()

private fun lastKnownLocation(context: Context): Pair<Double, Double>? {
    if (!granted(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
        !granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    ) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return runCatching {
        val providers = manager.getProviders(true)
        providers.asSequence()
            .mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    }.getOrNull()
}
