package com.example.foodtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.model.Ingredient

class IngredientsAdapter(
    private val ingredients: List<Ingredient>,
    private val userId: String,
    private val recipeTitle: String,
    private val onCheckboxChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder>() {

    private val checkboxStates = mutableMapOf<String, Boolean>()

    fun setCheckboxStates(states: Map<String, Boolean>) {
        checkboxStates.clear()
        checkboxStates.putAll(states)
        notifyDataSetChanged()
    }

    inner class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.ingredientCheckbox)

        fun bind(ingredient: Ingredient, isChecked: Boolean) {
            checkbox.text = "${ingredient.amount} ${ingredient.unit} ${ingredient.name}"

            // Set checkbox state without triggering listener
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = isChecked

            // Set the listener after setting the initial state
            checkbox.setOnCheckedChangeListener { _, checked ->
                checkboxStates[ingredient.name] = checked
                onCheckboxChanged(ingredient.name, checked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient_checkbox, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = ingredients[position]
        val isChecked = checkboxStates[ingredient.name] ?: false
        holder.bind(ingredient, isChecked)
    }

    override fun getItemCount(): Int = ingredients.size
}