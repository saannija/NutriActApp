package com.example.foodtracker.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val expirationDate: Timestamp? = null,
    val title: String = "",
    val message: String = "",
    val type: String = "EXPIRY_WARNING", // EXPIRY_WARNING, EXPIRED
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val daysUntilExpiry: Int = 0
)