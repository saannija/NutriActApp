package com.example.foodtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodtracker.model.SavedRecipe

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: SavedRecipe)

    @Query("SELECT * FROM saved_recipes WHERE userId = :userId")
    fun getSavedRecipesForUser(userId: String): List<SavedRecipe>

    @Query("SELECT COUNT(*) FROM saved_recipes WHERE userId = :userId AND title = :title")
    fun isRecipeSaved(userId: String, title: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredientCheckboxState(state: IngredientCheckboxState)

    @Query("SELECT * FROM ingredient_checkbox_states WHERE userId = :userId AND recipeTitle = :recipeTitle")
    suspend fun getIngredientCheckboxStates(userId: String, recipeTitle: String): List<IngredientCheckboxState>

    @Delete
    suspend fun deleteIngredientCheckboxState(state: IngredientCheckboxState)
}
