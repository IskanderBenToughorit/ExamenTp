package com.example.examentp.data

import java.io.Serializable

data class Contact(
    val id: Long = 0L,
    var firstName: String,
    var lastName: String,
    var phoneNumber: String,
    var email: String? = null,
    var photoUri: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var isFavorite: Boolean = false
) : Serializable {
    val fullName: String
        get() = "$lastName $firstName"
}
