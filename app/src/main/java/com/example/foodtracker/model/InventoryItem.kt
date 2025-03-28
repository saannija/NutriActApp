package com.example.foodtracker.model

import com.example.foodtracker.R
import com.google.firebase.Timestamp

data class InventoryItem(
    val productName: String,
    val expirationDate: Timestamp?,
    val category: String? = null,
    var iconResId: Int = R.drawable.ic_default,
    val documentId: String
)