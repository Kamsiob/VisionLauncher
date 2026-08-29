package io.github.kamsiob.launcher.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.kamsiob.launcher.R
import io.github.kamsiob.launcher.data.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    // Off the main thread. Walking the whole address book and sorting it with a
    // Collator during composition is the same stall the app list had, and an
    // address book can be far larger than an app list.
    val all by produceState(initialValue = emptyList<ContactsRepository.Contact>(), contacts) {
        value = withContext(Dispatchers.IO) { contacts.allContacts() }
    }
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
