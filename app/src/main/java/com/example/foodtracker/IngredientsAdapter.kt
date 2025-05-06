package com.example.foodtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.model.Ingredient

class IngredientsAdapter(private val ingredients: List<Ingredient>) :
    RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder>() {

    inner class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.ingredientCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient_checkbox, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = ingredients[position]
        holder.checkbox.text = "${ingredient.amount} ${ingredient.unit} ${ingredient.name}"
        holder.checkbox.isChecked = false // Can later be bound to pantry/fridge match
    }

    override fun getItemCount(): Int = ingredients.size
}
