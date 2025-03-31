package com.example.foodtracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_recipes")
data class SavedRecipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val prepTime: String?,
    val ingredients: String,
    val instructions: String,
    val imageUrl: String?,
    val servings: String?
)