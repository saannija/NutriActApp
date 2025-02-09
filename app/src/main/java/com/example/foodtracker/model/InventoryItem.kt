package com.example.foodtracker.model

import com.google.firebase.Timestamp

data class InventoryItem(
    val productName: String = "",
    val expirationDate: Timestamp? = null
)