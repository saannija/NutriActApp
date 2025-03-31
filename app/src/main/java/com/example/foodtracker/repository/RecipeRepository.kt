package com.example.foodtracker.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.foodtracker.data.AppDatabase
import com.example.foodtracker.model.Recipe
import com.example.foodtracker.model.SavedRecipe
import com.example.foodtracker.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecipeRepository(context: Context) {
    private val recipeApi = RetrofitClient.instance
    private val savedRecipeDao = AppDatabase.getDatabase(context).savedRecipeDao()

    fun getRecipes(ingredients: String): LiveData<List<Recipe>> {
        val recipesLiveData = MutableLiveData<List<Recipe>>()
        Log.d("RecipeRepository", "Fetching recipes for: $ingredients")

        recipeApi.getRecipes(ingredients).enqueue(object : Callback<List<Recipe>> {
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                if (response.isSuccessful) {
                    val recipesFromApi = response.body() ?: emptyList()
                    val recipes = recipesFromApi.map { recipe ->
                        // Handle potential null servings and prepTime
                        val servingsValue = recipe.servings?.let {
                            it.replace(" Servings", "").toIntOrNull() ?: 0
                        } ?: 0

                        recipe.copy(
                            servings = servingsValue.toString(),
                            prepTime = recipe.prepTime ?: 0
                        )
                    }
                    recipesLiveData.value = recipes
                    Log.d("RecipeRepository", "Received recipes: $recipes")
                } else {
                    Log.e("RecipeRepository", "Error: ${response.code()}")
                    recipesLiveData.value = emptyList()
                }
            }

            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                Log.e("RecipeRepository", "Failure: ${t.message}")
                recipesLiveData.value = emptyList()
            }
        })
        return recipesLiveData
    }

    suspend fun saveRecipe(recipe: SavedRecipe) {
        savedRecipeDao.insertSavedRecipe(recipe)
    }

    fun getSavedRecipesForUser(userId: String): LiveData<List<SavedRecipe>> {
        return savedRecipeDao.getSavedRecipesForUser(userId)
    }
}