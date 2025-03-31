package com.example.foodtracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.model.Recipe
import com.example.foodtracker.model.SavedRecipe
import com.example.foodtracker.viewmodel.RecipeViewModel
import com.google.firebase.auth.FirebaseAuth

class SavedRecipesFragment : Fragment() {
    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var savedRecipesRecyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_saved_recipes, container, false)
        savedRecipesRecyclerView = view.findViewById(R.id.savedRecipesRecyclerView)

        adapter = RecipeAdapter(emptyList())
        savedRecipesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        savedRecipesRecyclerView.adapter = adapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        if (userId != null) {
            viewModel.getSavedRecipesForUser(userId).observe(viewLifecycleOwner, Observer { savedRecipes ->
                val recipes = savedRecipes.map { it.toRecipe() }
                adapter.updateRecipes(recipes)
            })
        } else {
            Log.w("SavedRecipesFragment", "User not logged in. Cannot display saved recipes.")
        }

        return view
    }

    // Extension function to convert SavedRecipe to Recipe
    private fun SavedRecipe.toRecipe(): Recipe {
        return Recipe(
            title = this.title,
            ingredients = this.ingredients,
            instructions = this.instructions,
            imageUrl = this.imageUrl,
            prepTime = this.prepTime?.toIntOrNull() ?: 0,
            servings = this.servings
        )
    }
}