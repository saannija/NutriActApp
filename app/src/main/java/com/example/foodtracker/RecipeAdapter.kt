package com.example.foodtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodtracker.model.Recipe

class RecipeAdapter(
    private val recipeList: List<Recipe>,
    private val onItemClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.recipeImage)
        val recipeTitle: TextView = itemView.findViewById(R.id.recipeTitle)
        val recipeDescription: TextView = itemView.findViewById(R.id.recipeDescription)
        val recipeInfo: TextView = itemView.findViewById(R.id.recipeInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.recipeTitle.text = recipe.title
        holder.recipeDescription.text = recipe.description
        holder.recipeInfo.text = "Prep: ${recipe.prepTime} min | Servings: ${recipe.servings}"

        Glide.with(holder.recipeImage.context)
            .load(recipe.imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .into(holder.recipeImage)

        holder.itemView.setOnClickListener {
            onItemClick(recipe)
        }
    }

    override fun getItemCount(): Int = recipeList.size
}
