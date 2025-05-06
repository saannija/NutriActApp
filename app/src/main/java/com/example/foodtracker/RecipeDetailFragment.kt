package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.foodtracker.databinding.FragmentRecipeDetailBinding
import com.example.foodtracker.model.Recipe

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var recipe: Recipe

    companion object {
        // Pass the selected recipe to this fragment
        fun newInstance(recipe: Recipe): RecipeDetailFragment {
            val fragment = RecipeDetailFragment()
            val args = Bundle()
            args.putParcelable("recipe", recipe)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get recipe data from arguments
        arguments?.let {
            recipe = it.getParcelable("recipe")!!
            populateRecipeDetails(recipe)
        }

        binding.saveRecipeButton.setOnClickListener {
            // Logic for saving the recipe
            Toast.makeText(requireContext(), getString(R.string.recipe_saved), Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateRecipeDetails(recipe: Recipe) {
        // Load image with Glide or Picasso
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
        binding.recipeDetailTags.text = getString(R.string.tags_format, recipe.tags.joinToString(", "))

        // Ingredients
        val ingredients = recipe.ingredients.joinToString("\n") {
            "${it.amount} ${it.unit} of ${it.name}"
        }
        binding.recipeDetailIngredients.text = ingredients

        // Instructions
        val instructions = recipe.instructions.joinToString("\n") { it }
        binding.recipeDetailInstructions.text = instructions
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
