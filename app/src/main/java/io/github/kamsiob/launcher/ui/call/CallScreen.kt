package io.github.kamsiob.launcher.ui.call

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.Favorite
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.KeyStyle
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.LineIcons

/**
 * Grid 04. People first, relationship under the name. The Emergency key is
 * the only red in the app, and it opens the Emergency screen; it never dials.
 */
@Composable
fun CallScreen(
    favorites: List<Favorite>,
    hasContactsPermission: Boolean,
    onAllContacts: () -> Unit,
    onKeypad: () -> Unit,
    onEmergency: () -> Unit,
    onHome: () -> Unit,
    onContactsPermissionChanged: () -> Unit,
) {
    val context = LocalContext.current
    var permissionAsks by remember { mutableIntStateOf(0) }
    val requestContacts = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> onContactsPermissionChanged() }

    ScreenFrame(topBar = { TopBar(onHome = onHome) }) {
        ScreenTitle(stringResource(R.string.call_title))

        favorites.forEach { favorite ->
            val description = if (favorite.relationship.isBlank()) {
                stringResource(R.string.a11y_call_person_no_relation, favorite.name)
            } else {
                stringResource(R.string.a11y_call_person, favorite.name, favorite.relationship)
            }
            RowKey(
                label = favorite.name,
                meta = favorite.relationship.ifBlank { null },
                avatarInitial = initialOf(favorite.name),
                avatarColor = avatarColorFor(favorite.name),
                contentDescription = description,
                committing = true,
                onClick = { placeCall(context, favorite.number) },
            )
        }
        if (favorites.isEmpty() && hasContactsPermission) {
            NoteText(stringResource(R.string.call_no_favorites))
        }

        if (hasContactsPermission) {
            RowKey(
                label = stringResource(R.string.call_all_contacts),
                icon = LineIcons.person,
                onClick = onAllContacts,
            )
        } else {
            NoteText(stringResource(R.string.call_no_contacts_permission))
            ApplianceKey(
                label = stringResource(R.string.call_grant_contacts),
                onClick = {
                    permissionAsks++
                    requestContacts.launch(Manifest.permission.READ_CONTACTS)
                },
            )
        }
        RowKey(
            label = stringResource(R.string.call_dial_a_number),
            icon = LineIcons.dialpad,
            onClick = onKeypad,
        )
        Spacer(modifier = Modifier.weight(1f))
        ApplianceKey(
            label = stringResource(R.string.call_emergency),
            onClick = onEmergency,
            style = KeyStyle.EMERGENCY,
            contentDescription = stringResource(R.string.a11y_emergency_key),
        )
    }
}
