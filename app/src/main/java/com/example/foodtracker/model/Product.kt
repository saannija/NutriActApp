package com.example.foodtracker.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val barcode: String? = null,
    val productName: String = "",
    val category: String = "",
    val type: String = "",
    val expirationDate: Timestamp? = null,
    val storageStatus: String = "Unopened",
    val addedDate: Timestamp = Timestamp.now(),
    val updatedDate: Timestamp = Timestamp.now(),
    val openedDate: Timestamp? = null,
    val quantity: Int = 0,
    val unit: String = "",
    val totalAmount: Int = 0,
    val notes: String = "",
    val allergenAlert: Boolean = false,
    val deleted: Boolean = false,
    val masterProductId: String? = null
)