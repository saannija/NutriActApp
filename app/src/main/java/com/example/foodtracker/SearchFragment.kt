package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.viewmodel.RecipeViewModel

class SearchFragment : Fragment() {
    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        recipeRecyclerView = view.findViewById(R.id.recipeRecyclerView)

        adapter = RecipeAdapter(emptyList())
        recipeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recipeRecyclerView.adapter = adapter

        searchButton.setOnClickListener {
            val ingredients = searchEditText.text.toString()
            if (ingredients.isNotEmpty()) {
                viewModel.getRecipes(ingredients).observe(viewLifecycleOwner, Observer { recipes ->
                    adapter.updateRecipes(recipes)
                })
            }
        }

        return view
    }
}