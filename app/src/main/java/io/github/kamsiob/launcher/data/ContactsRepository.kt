package io.github.kamsiob.launcher.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.text.Collator

/**
 * Reads the device contacts for the Call screen. Read only, on demand, and
 * nothing is copied anywhere: the contacts live where they always lived.
 */
class ContactsRepository(private val context: Context) {

    data class Contact(
        val id: Long,
        val name: String,
        val number: String,
        val starred: Boolean,
    )

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    fun allContacts(): List<Contact> {
        if (!hasPermission()) return emptyList()
        val seen = LinkedHashMap<Long, Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val starredCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                if (id in seen) continue
                val name = cursor.getString(nameCol) ?: continue
                val number = cursor.getString(numberCol) ?: continue
                seen[id] = Contact(
                    id = id,
                    name = name,
                    number = number,
                    starred = cursor.getInt(starredCol) == 1,
                )
            }
        }
        val collator = Collator.getInstance()
        return seen.values.sortedWith(compareBy(collator) { it.name })
    }
}
