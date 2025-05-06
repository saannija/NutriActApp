package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.foodtracker.data.AppDatabase
import com.example.foodtracker.databinding.FragmentRecipeDetailBinding
import com.example.foodtracker.model.Recipe
import com.example.foodtracker.model.SavedRecipe
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var recipe: Recipe
    private var isFromSaved: Boolean = false

    companion object {
        fun newInstance(recipe: Recipe, fromSaved: Boolean = false): RecipeDetailFragment {
            val fragment = RecipeDetailFragment()
            val args = Bundle()
            args.putParcelable("recipe", recipe)
            args.putBoolean("fromSaved", fromSaved)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            recipe = it.getParcelable("recipe")!!
            isFromSaved = it.getBoolean("fromSaved", false)
            populateRecipeDetails(recipe)
            binding.ingredientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.ingredientsRecyclerView.adapter = IngredientsAdapter(recipe.ingredients)

        }

        if (isFromSaved) {
            binding.saveRecipeButton.visibility = View.GONE
        } else {
            checkIfRecipeSaved()
        }

        binding.saveRecipeButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val savedRecipe = SavedRecipe(
                userId = userId,
                title = recipe.title,
                description = recipe.description,
                imageUrl = recipe.imageUrl,
                category = recipe.category,
                cuisine = recipe.cuisine,
                tags = recipe.tags,
                prepTime = recipe.prepTime,
                cookingTime = recipe.cookingTime,
                totalTime = recipe.totalTime,
                servings = recipe.servings,
                ingredients = recipe.ingredients,
                instructions = recipe.instructions
            )

            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext()).recipeDao().insertRecipe(savedRecipe)
                Toast.makeText(requireContext(), getString(R.string.recipe_saved), Toast.LENGTH_SHORT).show()
                binding.saveRecipeButton.text = getString(R.string.recipe_already_saved)
                binding.saveRecipeButton.isEnabled = false
            }
        }
    }

    private fun checkIfRecipeSaved() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            val isSaved = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext())
                    .recipeDao()
                    .isRecipeSaved(userId, recipe.title) > 0
            }
            if (isSaved) {
                binding.saveRecipeButton.text = getString(R.string.recipe_already_saved)
                binding.saveRecipeButton.isEnabled = false
            }
        }
    }

    private fun populateRecipeDetails(recipe: Recipe) {
        Glide.with(requireContext())
            .load(recipe.imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .into(binding.recipeDetailImage)

        binding.recipeDetailTitle.text = recipe.title
        binding.recipeDetailDescription.text = recipe.description
        binding.recipeDetailPrepTime.text = getString(R.string.prep_time_format, recipe.prepTime)
        binding.recipeDetailTotalTime.text = getString(R.string.total_time_format, recipe.totalTime)
        binding.recipeDetailCategory.text = getString(R.string.category_format, recipe.category)
        binding.recipeDetailCuisine.text = getString(R.string.cuisine_format, recipe.cuisine)
        binding.recipeDetailServings.text = getString(R.string.servings_format, recipe.servings)

        binding.recipeDetailTags.removeAllViews()
        recipe.tags.forEach { tag ->
            val chip = Chip(requireContext())
            chip.text = tag
            chip.isClickable = false
            chip.isCheckable = false
            binding.recipeDetailTags.addView(chip)
        }

        // Numbered instructions
        val instructions = recipe.instructions.mapIndexed { index, step ->
            "${index + 1}. $step"
        }.joinToString("\n\n")
        binding.recipeDetailInstructions.text = instructions
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
