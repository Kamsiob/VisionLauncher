package io.github.kamsiob.launcher.ui.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
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
    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.emergency_title))
        ApplianceKey(
            label = stringResource(R.string.emergency_call_911),
            sublabel = stringResource(R.string.emergency_call_911_sub),
            onClick = { dialEmergency(context) },
            style = KeyStyle.EMERGENCY,
            minHeight = Dimens.bigKey,
            committing = true,
        )
        if (emergencyContact != null) {
            ApplianceKey(
                label = stringResource(R.string.emergency_alert_person, emergencyContact.name),
                sublabel = stringResource(R.string.emergency_alert_person_sub),
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
 * The dialer opens with 911 ready. ACTION_DIAL is the sanctioned handoff a
 * non dialer app has; the phone's own emergency machinery takes over from
 * there.
 */
private fun dialEmergency(context: Context) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
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
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
        == PackageManager.PERMISSION_GRANTED
    ) {
        runCatching {
            val sms = context.getSystemService(SmsManager::class.java)
            sms.sendMultipartTextMessage(
                person.number, null, sms.divideMessage(body), null, null,
            )
        }
    }
    placeCall(context, person.number)
}

private fun lastKnownLocation(context: Context): Pair<Double, Double>? {
    val fineGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return runCatching {
        val providers = manager.getProviders(true)
        providers.asSequence()
            .mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    }.getOrNull()
}
