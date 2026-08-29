package io.github.kamsiob.launcher.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.ContactsRepository
import io.github.kamsiob.launcher.data.EmergencyContact
import io.github.kamsiob.launcher.data.Favorite
import io.github.kamsiob.launcher.ui.call.avatarColorFor
import io.github.kamsiob.launcher.ui.call.initialOf
import io.github.kamsiob.launcher.ui.components.ApplianceKey
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.SectLabel
import io.github.kamsiob.launcher.ui.components.StatusPill
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.components.UndoStrip
import io.github.kamsiob.launcher.ui.theme.Dimens
import io.github.kamsiob.launcher.ui.theme.LineIcons
import io.github.kamsiob.launcher.ui.theme.LocalPalette
import io.github.kamsiob.launcher.ui.theme.TypeScale
import io.github.kamsiob.launcher.ui.theme.bodyStyle
import kotlinx.coroutines.delay

/**
 * Helper settings. Stage 1 holds favorites with their relationships, the
 * emergency person, restoring all threshold warnings, and About. The reply
 * phrases, the PIN, the printable sheet, and the settings file arrive with
 * their own stages.
 */
@Composable
fun HelperScreen(
    favorites: List<Favorite>,
    emergencyContact: EmergencyContact?,
    onSetFavorites: (List<Favorite>) -> Unit,
    onSetEmergencyContact: (EmergencyContact?) -> Unit,
    onRestoreWarnings: () -> Unit,
    onAddFavorite: () -> Unit,
    onChooseEmergencyPerson: () -> Unit,
    onAbout: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    var removed by remember { mutableStateOf<Pair<Favorite, Int>?>(null) }
    var warningsRestored by remember { mutableStateOf(false) }
    LaunchedEffect(warningsRestored) {
        if (warningsRestored) {
            delay(6000)
            warningsRestored = false
        }
    }

    ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = onBack) }) {
        ScreenTitle(stringResource(R.string.helper_title))

        SectLabel(stringResource(R.string.helper_favorites))
        favorites.forEachIndexed { index, favorite ->
            RowKey(
                label = favorite.name,
                meta = favorite.relationship.ifBlank { null },
                avatarInitial = initialOf(favorite.name),
                avatarColor = avatarColorFor(favorite.name),
                contentDescription = stringResource(R.string.helper_remove_favorite, favorite.name),
                committing = true,
                onClick = {
                    removed = favorite to index
                    onSetFavorites(favorites.filterIndexed { i, _ -> i != index })
                },
            )
        }
        removed?.let { (favorite, index) ->
            UndoStrip(
                message = stringResource(R.string.helper_favorite_removed, favorite.name),
                actionLabel = stringResource(R.string.put_it_back),
                onAction = {
                    val restoredList = favorites.toMutableList().apply {
                        add(index.coerceAtMost(size), favorite)
                    }
                    onSetFavorites(restoredList)
                    removed = null
                },
            )
        }
        ApplianceKey(
            label = stringResource(R.string.helper_add_favorite),
            onClick = onAddFavorite,
            minHeight = Dimens.keySmall,
            fontSize = TypeScale.keyLabelSmall,
        )

        SectLabel(stringResource(R.string.helper_emergency_person))
        RowKey(
            label = emergencyContact?.name ?: stringResource(R.string.emergency_no_person),
            meta = stringResource(R.string.helper_emergency_person_sub),
            icon = LineIcons.shield,
            onClick = onChooseEmergencyPerson,
        )

        RowKey(
            label = stringResource(R.string.helper_restore_warnings),
            meta = stringResource(R.string.helper_restore_warnings_sub),
            icon = LineIcons.restore,
            committing = true,
            onClick = {
                onRestoreWarnings()
                warningsRestored = true
            },
        )
        if (warningsRestored) {
            StatusPill(text = stringResource(R.string.helper_warnings_restored), announce = true)
        }
        RowKey(
            label = stringResource(R.string.helper_about),
            icon = LineIcons.door,
            onClick = onAbout,
        )
    }
}

/**
 * Picking a person from the contacts, used for both favorites and the
 * emergency person. Same large rows as everywhere else.
 */
@Composable
fun PickContactScreen(
    contacts: ContactsRepository,
    title: String,
    askRelationship: Boolean,
    onPicked: (name: String, number: String, relationship: String) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalPalette.current
    // Keyed on a counter so granting the permission re-reads instead of
    // leaving the screen empty forever.
    var reads by remember { mutableIntStateOf(0) }
    val permitted = remember(reads) { contacts.hasPermission() }
    val all = remember(reads) { contacts.allContacts() }
    val requestContacts = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { reads++ }
    var pending by remember { mutableStateOf<ContactsRepository.Contact?>(null) }
    var relationship by remember { mutableStateOf("") }

    val relationshipLabel = stringResource(R.string.a11y_relationship_field)
    val chosen = pending
    if (chosen != null && askRelationship) {
        ScreenFrame(topBar = { TopBar(onHome = onHome, onBack = { pending = null }) }) {
            ScreenTitle(chosen.name)
            OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                textStyle = bodyStyle(size = TypeScale.keyLabelSmall, weight = FontWeight.Medium),
                placeholder = {
                    Text(
                        text = stringResource(R.string.helper_relationship_hint),
                        style = bodyStyle(size = TypeScale.keyLabelSmall, weight = FontWeight.Medium),
                        color = palette.textSoft,
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.text,
                    unfocusedTextColor = palette.text,
                    focusedContainerColor = palette.card,
                    unfocusedContainerColor = palette.card,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.hairline,
                    cursorColor = palette.accent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = relationshipLabel },
            )
            ApplianceKey(
                label = stringResource(R.string.key_done),
                onClick = { onPicked(chosen.name, chosen.number, relationship) },
                committing = true,
            )
        }
        return
    }

    ScreenFrame(scrollable = false) {
        TopBar(onHome = onHome, onBack = onBack)
        ScreenTitle(title)
        // Without these two the screen was a title over empty space, with no
        // sentence, no way to grant, and no way forward. Setting an emergency
        // person was then impossible and nothing said why.
        if (!permitted) {
            NoteText(stringResource(R.string.call_no_contacts_permission))
            ApplianceKey(
                label = stringResource(R.string.call_grant_contacts),
                onClick = { requestContacts.launch(Manifest.permission.READ_CONTACTS) },
            )
        } else if (all.isEmpty()) {
            NoteText(stringResource(R.string.contacts_none))
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        ) {
            items(all, key = { it.id }) { contact ->
                RowKey(
                    label = contact.name,
                    avatarInitial = initialOf(contact.name),
                    avatarColor = avatarColorFor(contact.name),
                    // Only a commit when the pick finishes here. On the
                    // relationship path this row just opens the next step and
                    // the Done key there does the confirming.
                    committing = !askRelationship,
                    onClick = {
                        if (askRelationship) {
                            pending = contact
                        } else {
                            onPicked(contact.name, contact.number, "")
                        }
                    },
                )
            }
        }
    }
}
