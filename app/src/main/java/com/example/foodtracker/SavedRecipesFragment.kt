package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        binding.searchEditText.visibility = View.GONE
        binding.searchButton.visibility = View.GONE

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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Thread {
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
                adapter.notifyDataSetChanged()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
