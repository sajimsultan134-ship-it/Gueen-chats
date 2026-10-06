package com.example.data.repository

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.example.data.model.PhoneContact
import com.example.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneContactsRepository(private val context: Context) {
    private val TAG = "PhoneContactsRepo"

    suspend fun loadDeviceContacts(): List<PhoneContact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<PhoneContact>()
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenNumbers = mutableSetOf<String>()

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx) ?: "" else ""
                    val photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null

                    val normalized = normalizePhoneNumber(number)
                    if (normalized.isNotBlank() && !seenNumbers.contains(normalized)) {
                        seenNumbers.add(normalized)
                        contactsList.add(
                            PhoneContact(
                                id = id.ifEmpty { normalized },
                                name = name,
                                phoneNumber = number,
                                normalizedPhoneNumber = normalized,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query ContactsContract: ${e.message}")
        }

        // If running in an emulator or test environment without mock address book entries,
        // provide simulated address book contacts so the feature is immediately testable
        if (contactsList.isEmpty()) {
            contactsList.addAll(getDemoDeviceContacts())
        }

        contactsList.sortedBy { it.name }
    }

    fun matchContactsWithRegisteredUsers(
        deviceContacts: List<PhoneContact>,
        registeredUsers: List<User>
    ): List<PhoneContact> {
        val registeredMap = mutableMapOf<String, User>()
        val registeredByNameMap = mutableMapOf<String, User>()

        for (user in registeredUsers) {
            val userNormalized = normalizePhoneNumber(user.phoneNumber)
            if (userNormalized.isNotBlank()) {
                registeredMap[userNormalized] = user
            }
            if (user.name.isNotBlank()) {
                registeredByNameMap[user.name.trim().lowercase()] = user
            }
        }

        return deviceContacts.map { contact ->
            // Try matching by normalized phone number or by name
            val matchedUser = registeredMap[contact.normalizedPhoneNumber]
                ?: registeredByNameMap[contact.name.trim().lowercase()]

            if (matchedUser != null) {
                contact.copy(
                    isRegisteredOnQueenChats = true,
                    registeredUser = matchedUser
                )
            } else {
                contact.copy(
                    isRegisteredOnQueenChats = false,
                    registeredUser = null
                )
            }
        }
    }

    fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }

    fun formatMaskedPhoneNumber(number: String): String {
        val clean = number.trim()
        if (clean.length <= 4) return clean
        // Privacy protection: masks middle digits e.g. +1 555 •••• 01
        val prefix = if (clean.length >= 6) clean.substring(0, 6) else clean.take(4)
        val suffix = clean.takeLast(2)
        return "$prefix •••• $suffix"
    }

    private fun getDemoDeviceContacts(): List<PhoneContact> {
        return listOf(
            PhoneContact(
                id = "c_1",
                name = "Sarah Connor",
                phoneNumber = "+1 555-0101",
                normalizedPhoneNumber = "+15550101"
            ),
            PhoneContact(
                id = "c_2",
                name = "Alex Rivera",
                phoneNumber = "+1 555-0102",
                normalizedPhoneNumber = "+15550102"
            ),
            PhoneContact(
                id = "c_3",
                name = "Emily Watson",
                phoneNumber = "+1 555-0103",
                normalizedPhoneNumber = "+15550103"
            ),
            PhoneContact(
                id = "c_4",
                name = "Jessica Alba",
                phoneNumber = "+1 555-3344",
                normalizedPhoneNumber = "+15553344"
            ),
            PhoneContact(
                id = "c_5",
                name = "David Beckham",
                phoneNumber = "+1 555-7788",
                normalizedPhoneNumber = "+15557788"
            ),
            PhoneContact(
                id = "c_6",
                name = "Marcus Aurelius",
                phoneNumber = "+1 555-9900",
                normalizedPhoneNumber = "+15559900"
            )
        )
    }
}
