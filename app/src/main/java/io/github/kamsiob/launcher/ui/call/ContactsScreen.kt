package io.github.kamsiob.launcher.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.ContactsRepository
import io.github.kamsiob.launcher.ui.components.NoteText
import io.github.kamsiob.launcher.ui.components.RowKey
import io.github.kamsiob.launcher.ui.components.ScreenFrame
import io.github.kamsiob.launcher.ui.components.ScreenTitle
import io.github.kamsiob.launcher.ui.components.TopBar
import io.github.kamsiob.launcher.ui.theme.Dimens

/** All contacts, alphabetical, large rows. Tapping a person calls them. */
@Composable
fun ContactsScreen(
    contacts: ContactsRepository,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val all = remember(contacts) { contacts.allContacts() }
    ScreenFrame(scrollable = false) {
        TopBar(onHome = onHome, onBack = onBack)
        ScreenTitle(stringResource(R.string.call_all_contacts))
        if (all.isEmpty()) NoteText(stringResource(R.string.contacts_none))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap),
        ) {
            items(all, key = { it.id }) { contact ->
                RowKey(
                    label = contact.name,
                    avatarInitial = initialOf(contact.name),
                    avatarColor = avatarColorFor(contact.name),
                    contentDescription = stringResource(
                        R.string.a11y_call_person_no_relation, contact.name
                    ),
                    committing = true,
                    onClick = { placeCall(context, contact.number) },
                )
            }
        }
    }
}
