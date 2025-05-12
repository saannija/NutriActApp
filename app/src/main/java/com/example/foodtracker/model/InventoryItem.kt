package com.example.foodtracker.model

import com.example.foodtracker.R
import com.google.firebase.Timestamp

data class InventoryItem(
    val productName: String,
    val expirationDate: Timestamp?,
    val category: String? = null,
    var iconResId: Int = R.drawable.ic_default,
    val documentId: String,
    val deleted: Boolean = false,
    val type: String? = null,
    val storageStatus: String? = null,
    val quantity: Int = 0,
    val unit: String? = null,
    val totalAmount: Int = 0,
    val notes: String? = null,
    val allergenAlert: Boolean = false
)