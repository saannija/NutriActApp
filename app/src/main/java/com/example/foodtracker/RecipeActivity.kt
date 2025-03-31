package com.example.foodtracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.viewmodel.RecipeViewModel

class RecipeActivity : AppCompatActivity() {
    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)

        recyclerView = findViewById(R.id.recipeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val ingredients = "chicken, tomato" // Change this!!!
        viewModel.getRecipes(ingredients).observe(this, Observer { recipes ->
            recipeAdapter = RecipeAdapter(recipes)
            recyclerView.adapter = recipeAdapter
        })
    }
}