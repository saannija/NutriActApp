package com.example.foodtracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.foodtracker.util.Converters

@Entity(tableName = "saved_recipes")
@TypeConverters(Converters::class)
data class SavedRecipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val cuisine: String,
    val tags: List<String>,
    val prepTime: Int,
    val cookingTime: Int,
    val totalTime: Int,
    val servings: Int,
    val ingredients: List<Ingredient>,
    val instructions: List<String>
)
