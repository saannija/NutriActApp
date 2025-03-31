package com.example.foodtracker.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodtracker.model.SavedRecipe

@Dao
interface SavedRecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipe(recipe: SavedRecipe)

    @Query("SELECT * FROM saved_recipes WHERE userId = :userId")
    fun getSavedRecipesForUser(userId: String): LiveData<List<SavedRecipe>>
}