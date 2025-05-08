package com.example.foodtracker.model

import com.google.firebase.firestore.DocumentId

data class MasterProduct(
    @DocumentId val id: String = "",
    val barcode: String = "",
    val productName: String = "",
    val brand: String = "",
    val category: String = "",
    val type: String = "",
    val quantity: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val allergens: List<String> = emptyList()
)