package com.example.foodtracker

import RecipeDiffCallback
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodtracker.model.Recipe


class RecipeAdapter(private var recipes: List<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.recipeImage)
        val title: TextView = view.findViewById(R.id.recipeTitle)
        val info: TextView = view.findViewById(R.id.recipeInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.title.text = recipe.title
        "Prep Time: ${recipe.prepTime ?: 0} min | Servings: ${recipe.servings ?: "N/A"}".also { holder.info.text = it }

        // Load image with Glide (if available)
        if (recipe.imageUrl != null && recipe.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_placeholder)
        }

        // Click event to open full recipe
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RecipeDetailActivity::class.java).apply {
                putExtra("RECIPE_TITLE", recipe.title)
                putExtra("RECIPE_PREP_TIME", recipe.prepTime.toString())
                putExtra("RECIPE_INGREDIENTS", recipe.ingredients)
                putExtra("RECIPE_INSTRUCTIONS", recipe.instructions)
                putExtra("RECIPE_IMAGE", recipe.imageUrl)
                putExtra("RECIPE_SERVINGS", recipe.servings)

            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = recipes.size

    fun updateRecipes(newRecipes: List<Recipe>) {
        val diffResult = DiffUtil.calculateDiff(RecipeDiffCallback(recipes, newRecipes))
        recipes = newRecipes
        diffResult.dispatchUpdatesTo(this)
    }
}