package com.example.foodtracker

import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.foodtracker.model.SavedRecipe
import com.example.foodtracker.viewmodel.RecipeViewModel
import com.google.firebase.auth.FirebaseAuth

class RecipeDetailActivity : AppCompatActivity() {
    private val viewModel: RecipeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        val title = intent.getStringExtra("RECIPE_TITLE") ?: ""
        val prepTime = intent.getStringExtra("RECIPE_PREP_TIME") ?: ""
        val ingredients = intent.getStringExtra("RECIPE_INGREDIENTS") ?: ""
        val instructions = intent.getStringExtra("RECIPE_INSTRUCTIONS") ?: ""
        val imageUrl = intent.getStringExtra("RECIPE_IMAGE")
        val servings = intent.getStringExtra("RECIPE_SERVINGS") ?: ""

        findViewById<TextView>(R.id.recipeDetailTitle).text = title
        findViewById<TextView>(R.id.recipeDetailPrepTime).text = "Prep Time: $prepTime minutes"
        findViewById<TextView>(R.id.recipeDetailServings).text = "Servings: $servings"

        // Apply improved formatting to ingredients and instructions using HTML
        findViewById<TextView>(R.id.recipeDetailIngredients).text = formatIngredients(ingredients)
        findViewById<TextView>(R.id.recipeDetailInstructions).text = formatInstructions(instructions)

        val imageView: ImageView = findViewById(R.id.recipeDetailImage)
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_placeholder)
        }

        val saveButton: Button = findViewById(R.id.saveRecipeButton)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        if (userId != null) {
            viewModel.getSavedRecipesForUser(userId).observe(this) { savedRecipes ->
                val isRecipeSaved = savedRecipes.any { it.title == title }
                saveButton.visibility = if (isRecipeSaved) View.GONE else View.VISIBLE
            }
        } else {
            saveButton.visibility = View.GONE
        }

        saveButton.setOnClickListener {
            if (userId != null) {
                val savedRecipe = SavedRecipe(
                    userId = userId,
                    title = title,
                    prepTime = prepTime,
                    ingredients = ingredients,
                    instructions = instructions,
                    imageUrl = imageUrl,
                    servings = servings
                )
                viewModel.saveRecipe(savedRecipe)
                Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show()
                saveButton.visibility = View.GONE
            } else {
                Toast.makeText(this, "Please log in to save recipes.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatIngredients(ingredients: String): CharSequence {
        val formattedIngredients = StringBuilder()
        formattedIngredients.append("<b>Ingredients:</b><br>")
        ingredients.split("|").forEach { // Split by "|"
            formattedIngredients.append("&#8226; ${it.trim()}<br>")
        }
        return Html.fromHtml(formattedIngredients.toString(), Html.FROM_HTML_MODE_LEGACY)
    }

    private fun formatInstructions(instructions: String): CharSequence {
        val formattedInstructions = StringBuilder()
        formattedInstructions.append("<b>Instructions:</b><br>")
        instructions.split(".").forEachIndexed { index, step ->
            formattedInstructions.append("${index + 1}. ${step.trim()}<br>")
        }
        return Html.fromHtml(formattedInstructions.toString(), Html.FROM_HTML_MODE_LEGACY)
    }
}