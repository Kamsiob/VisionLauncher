package io.github.kamsiob.launcher.ui.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.messages.hasNotificationAccess
import io.github.kamsiob.launcher.nav.SystemDestination
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.BodyText
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.SerifHeading
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette

/** Where onboarding currently stands. Order matters, see spec 5.11. */
private enum class Step { WELCOME, SET_HOME, CONTACTS, PHONE, CAMERA, MESSAGES, BATTERY, DONE }

/**
 * Grid 22 and 23. Reassurance first, then the home role, then permissions one
 * at a time with a plain reason each, then the battery step that decides
 * whether messaging still works in month six.
 */
@Composable
fun OnboardingScreen(
    onFinished: (helperPath: Boolean, batterySkipped: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(Step.WELCOME) }
    var helperPath by remember { mutableStateOf(false) }
    var batterySkipped by remember { mutableStateOf(false) }
    var skippedSomething by remember { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { step = Step.CONTACTS }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) skippedSomething = true
        step = Step.PHONE
    }
    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) skippedSomething = true
        step = Step.CAMERA
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) skippedSomething = true
        step = Step.MESSAGES
    }
    // Notification access is not a runtime permission. It is a switch on a
    // system screen, so there is no result to read and the state is checked on
    // the way back instead.
    val messagesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!hasNotificationAccess(context)) skippedSomething = true
        step = Step.BATTERY
    }
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        batterySkipped = !isIgnoringBatteryOptimizations(context)
        step = Step.DONE
    }

    when (step) {
        Step.WELCOME -> ScreenFrame(scrollable = false) {
            Spacer(modifier = Modifier.weight(1f))
            SerifHeading(stringResource(R.string.onboarding_cant_break), size = 46)
            BodyText(stringResource(R.string.onboarding_cant_break_body))
            Spacer(modifier = Modifier.weight(1f))
            ApplianceKey(
                label = stringResource(R.string.onboarding_own_phone),
                onClick = {
                    helperPath = false
                    step = Step.SET_HOME
                },
                minHeight = 110.dp,
            )
            ApplianceKey(
                label = stringResource(R.string.onboarding_helping),
                onClick = {
                    helperPath = true
                    step = Step.SET_HOME
                },
                minHeight = 110.dp,
            )
        }

        Step.SET_HOME -> StepScreen(
            icon = LineIcons.home,
            title = stringResource(R.string.onboarding_set_home_title),
            body = stringResource(R.string.onboarding_set_home_body),
            actionLabel = stringResource(R.string.onboarding_set_home_key),
            onAction = { roleLauncher.launch(homeRoleIntent(context)) },
            onSkip = {
                skippedSomething = true
                step = Step.CONTACTS
            },
        )

        Step.CONTACTS -> StepScreen(
            icon = LineIcons.person,
            title = stringResource(R.string.onboarding_contacts_title),
            body = stringResource(R.string.onboarding_contacts_body),
            actionLabel = stringResource(R.string.onboarding_contacts_key),
            onAction = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
            onSkip = {
                skippedSomething = true
                step = Step.PHONE
            },
        )

        Step.PHONE -> StepScreen(
            icon = LineIcons.call,
            title = stringResource(R.string.onboarding_phone_title),
            body = stringResource(R.string.onboarding_phone_body),
            actionLabel = stringResource(R.string.onboarding_phone_key),
            onAction = { phoneLauncher.launch(Manifest.permission.CALL_PHONE) },
            onSkip = {
                skippedSomething = true
                step = Step.CAMERA
            },
        )

        Step.CAMERA -> StepScreen(
            icon = LineIcons.magnifier,
            title = stringResource(R.string.onboarding_camera_title),
            body = stringResource(R.string.onboarding_camera_body),
            actionLabel = stringResource(R.string.onboarding_camera_key),
            onAction = { cameraLauncher.launch(Manifest.permission.CAMERA) },
            onSkip = {
                skippedSomething = true
                step = Step.MESSAGES
            },
        )

        // The step the whole messaging feature rests on. Without it messages
        // never arrive, and the first build never asked: somebody finished
        // setting the phone up and then waited for messages that could not
        // reach them.
        Step.MESSAGES -> StepScreen(
            icon = LineIcons.messages,
            title = stringResource(R.string.onboarding_messages_title),
            body = stringResource(
                if (hasNotificationAccess(context)) R.string.onboarding_messages_already
                else R.string.onboarding_messages_body
            ),
            actionLabel = stringResource(R.string.onboarding_messages_key),
            onAction = {
                val opened = runCatching {
                    messagesLauncher.launch(
                        SystemDestination.NOTIFICATION_ACCESS.intent(context)
                    )
                    true
                }.getOrDefault(false)
                // A phone with no such screen must not strand somebody on a key
                // that does nothing.
                if (!opened) {
                    skippedSomething = true
                    step = Step.BATTERY
                }
            },
            onSkip = {
                skippedSomething = true
                step = Step.BATTERY
            },
        )

        Step.BATTERY -> StepScreen(
            icon = LineIcons.messages,
            title = stringResource(R.string.onboarding_battery_title),
            body = stringResource(R.string.onboarding_battery_body),
            // No second line. It said "On the next screen, choose Don't
            // optimize", which is the wording of a different entry point. This
            // key raises the system's allow dialog, where that control does not
            // exist, so the sentence sent people hunting for nothing.
            actionLabel = stringResource(R.string.onboarding_battery_key),
            onAction = {
                batteryLauncher.launch(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    )
                )
            },
            onSkip = {
                batterySkipped = true
                step = Step.DONE
            },
            skipNote = stringResource(R.string.onboarding_battery_skipped),
        )

        Step.DONE -> ScreenFrame(scrollable = false) {
            Spacer(modifier = Modifier.weight(1f))
            SerifHeading(stringResource(R.string.onboarding_all_set))
            if (skippedSomething || batterySkipped) {
                BodyText(stringResource(R.string.onboarding_skipped_consequence))
            }
            if (helperPath) {
                BodyText(stringResource(R.string.onboarding_helper_note))
            }
            Spacer(modifier = Modifier.weight(1f))
            // The helper path ends where a helper's work actually is. Section
            // 5.11 asks this route to cover favorites, the reply phrases, the
            // code and the printable sheet; all four live on one screen, so it
            // offers that screen rather than rebuilding four of them here and
            // leaving two copies to drift apart.
            if (helperPath) {
                ApplianceKey(
                    label = stringResource(R.string.onboarding_go_helper),
                    onClick = { onFinished(true, batterySkipped) },
                    style = KeyStyle.PRIMARY,
                    committing = true,
                )
                ApplianceKey(
                    label = stringResource(R.string.onboarding_go_home_instead),
                    onClick = { onFinished(false, batterySkipped) },
                    minHeight = Dimens.keySmall,
                    fontSize = TypeScale.keyLabelSmall,
                )
            } else {
                ApplianceKey(
                    label = stringResource(R.string.onboarding_go_home),
                    onClick = { onFinished(false, batterySkipped) },
                    style = KeyStyle.PRIMARY,
                    committing = true,
                )
            }
        }
    }
}

@Composable
private fun StepScreen(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    onSkip: () -> Unit,
    secondBody: String? = null,
    skipNote: String? = null,
) {
    ScreenFrame(scrollable = false) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalPalette.current.accent,
            modifier = Modifier.size(Dimens.thresholdIcon),
        )
        SerifHeading(title)
        BodyText(body)
        if (secondBody != null) BodyText(secondBody)
        Spacer(modifier = Modifier.weight(1f))
        ApplianceKey(
            label = actionLabel,
            onClick = onAction,
            style = KeyStyle.PRIMARY,
        )
        ApplianceKey(
            label = stringResource(R.string.key_skip),
            onClick = onSkip,
        )
        if (skipNote != null) NoteText(skipNote)
    }
}

/** ROLE_HOME where the platform offers it, the home settings screen otherwise. */
fun homeRoleIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= 29) {
        val manager = context.getSystemService(RoleManager::class.java)
        if (manager != null &&
            manager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !manager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            return manager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(context.packageName)
