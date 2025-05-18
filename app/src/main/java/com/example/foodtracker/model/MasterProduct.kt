package com.example.foodtracker.model

import com.google.firebase.firestore.DocumentId

data class MasterProduct(
    @DocumentId val id: String = "",
    val barcode: String = "",
    val productName: String = "",
    val brand: String = "",
    val category: String = "",
    val type: String = "",
    val quantity: Int? = null,
    val unit: String = "",
    val imageUrl: String? = null,
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val allergens: List<String> = emptyList()
)