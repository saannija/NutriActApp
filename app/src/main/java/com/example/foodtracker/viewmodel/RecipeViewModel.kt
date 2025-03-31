package com.example.foodtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.foodtracker.model.Recipe
import com.example.foodtracker.model.SavedRecipe
import com.example.foodtracker.repository.RecipeRepository
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RecipeRepository(application)

    fun getRecipes(ingredients: String): LiveData<List<Recipe>> {
        return repository.getRecipes(ingredients)
    }

    fun saveRecipe(recipe: SavedRecipe) {
        viewModelScope.launch {
            repository.saveRecipe(recipe)
        }
    }

    fun getSavedRecipesForUser(userId: String): LiveData<List<SavedRecipe>> {
        return repository.getSavedRecipesForUser(userId)
    }
}