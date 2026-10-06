package com.example.data.model

data class PhoneContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val normalizedPhoneNumber: String,
    val photoUri: String? = null,
    val isRegisteredOnQueenChats: Boolean = false,
    val registeredUser: User? = null
)
