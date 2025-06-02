package com.example.foodtracker.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String? = null,
    val surname: String? = null,
    val username: String = "",
    val email: String = "",
    val phone: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {

    fun getDisplayName(): String {
        return when {
            !name.isNullOrEmpty() && !surname.isNullOrEmpty() -> "$name $surname"
            !name.isNullOrEmpty() -> name
            username.isNotEmpty() -> username
            else -> "User"
        }
    }

    fun getFullName(): String? {
        return if (!name.isNullOrEmpty() && !surname.isNullOrEmpty()) {
            "$name $surname"
        } else {
            null
        }
    }

    // Convert to HashMap for Firestore
    fun toHashMap(): HashMap<String, Any?> {
        return hashMapOf(
            "name" to name,
            "surname" to surname,
            "username" to username,
            "email" to email,
            "phone" to phone,
            "profileImageUrl" to profileImageUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        // Create User from Firestore document
        fun fromFirestore(uid: String, data: Map<String, Any>): User {
            return User(
                uid = uid,
                name = data["name"] as? String,
                surname = data["surname"] as? String,
                username = data["username"] as? String ?: "",
                email = data["email"] as? String ?: "",
                phone = data["phone"] as? String,
                profileImageUrl = data["profileImageUrl"] as? String,
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp
            )
        }
    }
}