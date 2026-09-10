package com.sozo.callmanager.data

import android.content.Context
import android.provider.ContactsContract

/**
 * Reads the device's contact list (READ_CONTACTS permission required,
 * requested in SetupScreen).
 */
object ContactsRepository {

    /**
     * Looks up a single contact's display name from a phone number — used to
     * show "who is this" during an active/incoming call, like a real caller-ID.
     * Returns null if the number doesn't match any saved contact.
     */
    fun lookupNameForNumber(context: Context, number: String): String? {
        if (number.isBlank()) return null
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameCol = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameCol >= 0) return cursor.getString(nameCol)
            }
        }
        return null
    }

    fun getContacts(context: Context): List<Contact> {
        val results = mutableListOf<Contact>()
        val seen = HashSet<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val number = it.getString(numberCol)?.trim() ?: continue
                val name = it.getString(nameCol) ?: number
                val id = it.getString(idCol) ?: number

                // A contact can have multiple identical rows across accounts — dedupe.
                val dedupeKey = "$name|$number"
                if (seen.add(dedupeKey)) {
                    results.add(Contact(id = id, name = name, number = number))
                }
            }
        }
        return results
    }
}
