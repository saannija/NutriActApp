package com.example.foodtracker

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodtracker.databinding.FragmentSearchBinding
import com.example.foodtracker.model.Recipe
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.Normalizer

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val allRecipes = mutableListOf<Recipe>()
    private val filteredRecipes = mutableListOf<Recipe>()
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchFunctionality()
        loadLatestRecipesFromFirestore()
    }

    private fun setupRecyclerView() {
        adapter = RecipeAdapter(filteredRecipes) { selectedRecipe ->
            // Navigate to detail fragment with selectedRecipe
            val fragment = RecipeDetailFragment.newInstance(selectedRecipe)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recipeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recipeRecyclerView.adapter = adapter
    }

    private fun setupSearchFunctionality() {
        // Real-time search as user types
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                performSearch(query)
            }
        })
    }

    private fun loadLatestRecipesFromFirestore() {
        showLoadingState()

        db.collection("recipes")
            .orderBy("dateAdded", Query.Direction.DESCENDING)
            .limit(10) // Get latest 10 recipes
            .get()
            .addOnSuccessListener { result ->
                allRecipes.clear()
                filteredRecipes.clear()

                if (result.isEmpty) {
                    showEmptyState("No recipes available.\n\nCheck back later for new recipes!")
                } else {
                    for (doc in result) {
                        try {
                            val recipe = doc.toObject(Recipe::class.java)
                            allRecipes.add(recipe)
                        } catch (e: Exception) {
                            // Skip malformed recipes
                            continue
                        }
                    }

                    if (allRecipes.isEmpty()) {
                        showEmptyState("No valid recipes found.\n\nCheck back later for new recipes!")
                    } else {
                        // Initially show all loaded recipes
                        filteredRecipes.addAll(allRecipes)
                        showRecipeList()
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                showEmptyState("Unable to load recipes.\n\nPlease check your internet connection and try again.")
                Toast.makeText(requireContext(), "Failed to load recipes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performSearch(query: String) {
        filteredRecipes.clear()

        if (query.isEmpty()) {
            // Show all recipes if search is empty
            filteredRecipes.addAll(allRecipes)
        } else {
            val normalizedQuery = normalizeText(query)

            for (recipe in allRecipes) {
                if (matchesSearchCriteria(recipe, normalizedQuery)) {
                    filteredRecipes.add(recipe)
                }
            }
        }

        if (filteredRecipes.isEmpty() && query.isNotEmpty()) {
            showEmptyState("No recipes found for \"$query\".\n\nTry searching with different keywords.")
        } else if (filteredRecipes.isEmpty()) {
            showEmptyState("No recipes available.\n\nCheck back later for new recipes!")
        } else {
            showRecipeList()
        }

        adapter.notifyDataSetChanged()
    }

    private fun matchesSearchCriteria(recipe: Recipe, normalizedQuery: String): Boolean {
        // Search by recipe title (case insensitive)
        val recipeTitle = normalizeText(recipe.title)
        if (recipeTitle.contains(normalizedQuery)) {
            return true
        }

        // Search by description
        val recipeDescription = normalizeText(recipe.description)
        if (recipeDescription.contains(normalizedQuery)) {
            return true
        }

        // Search by category
        val recipeCategory = normalizeText(recipe.category)
        if (recipeCategory.contains(normalizedQuery)) {
            return true
        }

        // Search by cuisine
        val recipeCuisine = normalizeText(recipe.cuisine)
        if (recipeCuisine.contains(normalizedQuery)) {
            return true
        }

        // Search by tags
        for (tag in recipe.tags) {
            val normalizedTag = normalizeText(tag)
            if (normalizedTag.contains(normalizedQuery)) {
                return true
            }
        }

        // Search by ingredients
        for (ingredient in recipe.ingredients) {
            val ingredientName = normalizeText(ingredient.name)
            if (ingredientName.contains(normalizedQuery)) {
                return true
            }

            val ingredientUnit = normalizeText(ingredient.unit)
            if (ingredientUnit.contains(normalizedQuery)) {
                return true
            }
        }

        return false
    }

    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}".toRegex(), "") // Remove diacritical marks
    }

    private fun showLoadingState() {
        binding.recipeRecyclerView.visibility = View.GONE
        hideEmptyState()
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
        hideEmptyState()
        binding.recipeRecyclerView.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        val emptyStateTextView = view?.findViewById<TextView>(R.id.emptyStateTextView)
        emptyStateTextView?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}