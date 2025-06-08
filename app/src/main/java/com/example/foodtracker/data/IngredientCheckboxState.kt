package com.example.foodtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient_checkbox_states")
data class IngredientCheckboxState(
    @PrimaryKey val id: String, // Combination of userId + recipeTitle + ingredientName
    val userId: String,
    val recipeTitle: String,
    val ingredientName: String,
    val isChecked: Boolean
)