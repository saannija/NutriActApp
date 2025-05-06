package com.example.foodtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.foodtracker.data.FirestoreService
import com.example.foodtracker.model.Recipe
import kotlinx.coroutines.launch

class RecipeViewModel : ViewModel() {

    private val firestoreService = FirestoreService()

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    fun fetchRecipes() {
        viewModelScope.launch {
            _recipes.value = firestoreService.getAllRecipes()
        }
    }
}
