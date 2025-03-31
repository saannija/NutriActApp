package com.example.foodtracker.model

import com.google.gson.annotations.SerializedName

data class Recipe(
    val title: String,
    val ingredients: String,
    val instructions: String,
    @SerializedName("image") val imageUrl: String?,
    val prepTime: Int?,
    val servings: String?
)
