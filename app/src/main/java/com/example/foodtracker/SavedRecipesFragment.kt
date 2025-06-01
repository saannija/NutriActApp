package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodtracker.data.AppDatabase
import com.example.foodtracker.databinding.FragmentSearchBinding
import com.example.foodtracker.model.Recipe
import com.google.firebase.auth.FirebaseAuth

class SavedRecipesFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecipeAdapter
    private val savedRecipeList = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide search components since this is for saved recipes
        binding.searchEditText.visibility = View.GONE

        adapter = RecipeAdapter(savedRecipeList) { selectedRecipe ->
            val fragment = RecipeDetailFragment.newInstance(selectedRecipe, fromSaved = true)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recipeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recipeRecyclerView.adapter = adapter

        loadSavedRecipes()
    }

    private fun loadSavedRecipes() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showEmptyState("Please log in to view your saved recipes.")
            return
        }

        Thread {
            try {
                val dao = AppDatabase.getDatabase(requireContext()).recipeDao()
                val savedRecipes = dao.getSavedRecipesForUser(userId)

                val recipes = savedRecipes.map {
                    Recipe(
                        title = it.title,
                        description = it.description,
                        imageUrl = it.imageUrl,
                        category = it.category,
                        cuisine = it.cuisine,
                        tags = it.tags,
                        prepTime = it.prepTime,
                        cookingTime = it.cookingTime,
                        totalTime = it.totalTime,
                        servings = it.servings,
                        ingredients = it.ingredients,
                        instructions = it.instructions
                    )
                }

                requireActivity().runOnUiThread {
                    savedRecipeList.clear()
                    savedRecipeList.addAll(recipes)

                    if (recipes.isEmpty()) {
                        showEmptyState("No saved recipes yet.\n\nExplore recipes in the Search tab and save your favorites!")
                    } else {
                        showRecipeList()
                    }
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    showEmptyState("Unable to load saved recipes.\nPlease try again later.")
                }
            }
        }.start()
    }

    private fun showEmptyState(message: String) {
        var emptyStateTextView = view?.findViewById<TextView>(R.id.emptyStateTextView)
        if (emptyStateTextView == null) {
            emptyStateTextView = TextView(requireContext()).apply {
                id = R.id.emptyStateTextView
                textSize = 16f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(32, 32, 32, 32)
            }
            (view as ViewGroup).addView(emptyStateTextView)
        }

        emptyStateTextView.text = message
        emptyStateTextView.visibility = View.VISIBLE
        binding.recipeRecyclerView.visibility = View.GONE
    }

    private fun showRecipeList() {
        val emptyStateTextView = view?.findViewById<TextView>(R.id.emptyStateTextView)
        emptyStateTextView?.visibility = View.GONE
        binding.recipeRecyclerView.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}