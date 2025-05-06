package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodtracker.databinding.FragmentSearchBinding
import com.example.foodtracker.model.Recipe
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val recipeList = mutableListOf<Recipe>()
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RecipeAdapter(recipeList) { selectedRecipe ->
            // Navigate to detail fragment with selectedRecipe
            val fragment = RecipeDetailFragment.newInstance(selectedRecipe)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.recipeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recipeRecyclerView.adapter = adapter

        loadRecipesFromFirestore()
    }

    private fun loadRecipesFromFirestore() {
        db.collection("recipes")
            .get()
            .addOnSuccessListener { result ->
                recipeList.clear()
                for (doc in result) {
                    val recipe = doc.toObject(Recipe::class.java)
                    recipeList.add(recipe)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load recipes", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
