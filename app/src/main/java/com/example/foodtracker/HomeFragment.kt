package com.example.foodtracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.foodtracker.data.AppDatabase
import com.example.foodtracker.databinding.FragmentHomeBinding
import com.example.foodtracker.databinding.ItemRecipeBinding
import com.example.foodtracker.model.Ingredient
import com.example.foodtracker.model.Product
import com.example.foodtracker.model.Recipe
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var expiringProductsAdapter: ExpiringProductsAdapter
    private lateinit var savedRecipesAdapter: RecipeAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Real data lists
    private val expiringProductsList = mutableListOf<Product>()
    private val savedRecipesList = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupAdapters()
        setupClickListeners()
        setupRecyclerViews()
        loadRealData()

        return view
    }

    private fun setupAdapters() {
        expiringProductsAdapter = ExpiringProductsAdapter { product ->
            // Navigate to AddFragment in edit mode with product data
            val addFragment = AddFragment().apply {
                arguments = Bundle().apply {
                    putString("documentId", product.id)
                    putString("productName", product.productName)
                    putString("category", product.category)
                    putString("type", product.type ?: "")
                    putInt("quantity", product.quantity)
                    putString("unit", product.unit)
                    putInt("totalAmount", product.totalAmount ?: 0)
                    putString("notes", product.notes ?: "")
                    putBoolean("allergenAlert", product.allergenAlert ?: false)
                    putString("storageStatus", product.storageStatus ?: "Unopened")

                    // Convert Timestamp to milliseconds for the date picker
                    product.expirationDate?.let { timestamp ->
                        putLong("expirationDate", timestamp.toDate().time)
                    }
                }
            }
            navigateToFragment(addFragment, "AddFragment")
        }

        savedRecipesAdapter = RecipeAdapter(savedRecipesList) { recipe ->
//            Toast.makeText(context, "Clicked on saved recipe: ${recipe.title}", Toast.LENGTH_SHORT).show()
            val detailFragment = RecipeDetailFragment.newInstance(recipe, true)
            navigateToFragment(detailFragment, "RecipeDetailFragment")
        }
    }

    private fun loadExpiringProducts(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 3)
        val weekFromNow = Timestamp(calendar.time)

        db.collection("products")
            .whereEqualTo("userId", userId)
            .whereEqualTo("deleted", false)
            .whereLessThanOrEqualTo("expirationDate", weekFromNow)
            .orderBy("expirationDate", Query.Direction.ASCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                val products = mutableListOf<Product>()
                for (document in documents) {
                    val product = Product(
                        id = document.id,
                        productName = document.getString("productName") ?: "",
                        category = document.getString("category") ?: "",
                        type = document.getString("type") ?: "",
                        expirationDate = document.getTimestamp("expirationDate"),
                        quantity = document.getLong("quantity")?.toInt() ?: 0,
                        unit = document.getString("unit") ?: "",
                        totalAmount = document.getLong("totalAmount")?.toInt() ?: 0,
                        notes = document.getString("notes") ?: "",
                        allergenAlert = document.getBoolean("allergenAlert") ?: false,
                        storageStatus = document.getString("storageStatus") ?: "Unopened"
                    )
                    products.add(product)
                }

                expiringProductsList.clear()
                expiringProductsList.addAll(products)

                if (products.isEmpty()) {
                    binding.noExpiringItemsTextView.visibility = View.VISIBLE
                    binding.noExpiringItemsTextView.text = "Great! No items expiring soon."
                    binding.expiringProductsRecyclerView.visibility = View.GONE
                } else {
                    binding.noExpiringItemsTextView.visibility = View.GONE
                    binding.expiringProductsRecyclerView.visibility = View.VISIBLE
                    expiringProductsAdapter.submitList(products)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("HomeFragment", "Error loading expiring products: ", exception)
                binding.noExpiringItemsTextView.visibility = View.VISIBLE
                binding.noExpiringItemsTextView.text = "Unable to load expiring items."
                binding.expiringProductsRecyclerView.visibility = View.GONE
            }
    }

    private fun setupClickListeners() {
        binding.quickAddScanButton.setOnClickListener {
            // Navigate to AddFragment and trigger barcode scanning
            val addFragment = AddFragment().apply {
                // Set a flag or argument to indicate we want to start scanning immediately
                arguments = Bundle().apply {
                    putBoolean("startScanning", true)
                }
            }
            navigateToFragment(addFragment, "AddFragment")
        }

        binding.quickAddManualButton.setOnClickListener {
            // Navigate to AddFragment for manual entry
            val addFragment = AddFragment().apply {
                // Set a flag or argument to indicate we want manual entry
                arguments = Bundle().apply {
                    putBoolean("showManualEntry", true)
                }
            }
            navigateToFragment(addFragment, "AddFragment")
        }

        binding.viewAllSavedRecipesButton.setOnClickListener {
            navigateToFragment(SavedRecipesFragment(), "SavedRecipesFragment")
        }

        binding.inventorySummaryCard.setOnClickListener {
            try {
                navigateToFragment(InventoryFragment(), "InventoryFragment")

            } catch (e: Exception) {
                Toast.makeText(context, "Unable to navigate to inventory", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToFragment(fragment: Fragment, tag: String? = null) {
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment, tag)
        transaction.addToBackStack(tag)
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        transaction.commit()
    }

    private fun setupRecyclerViews() {
        binding.expiringProductsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.expiringProductsRecyclerView.adapter = expiringProductsAdapter

        binding.savedRecipesPreviewRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.savedRecipesPreviewRecyclerView.adapter = savedRecipesAdapter
    }

    private fun loadRealData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("HomeFragment", "User not logged in.")
            showNotLoggedInState()
            return
        }

        // Set welcome message with user info
        val user = auth.currentUser
        val displayName = user?.displayName ?: "Foodie"
        binding.welcomeMessageTextView.text = "Welcome back, $displayName!"

        // Load all data
        loadExpiringProducts(userId)
        loadSavedRecipes(userId)
        loadInventorySummary(userId)
        loadRecommendedRecipe()
    }

    private fun loadSavedRecipes(userId: String) {
        Thread {
            try {
                val dao = AppDatabase.getDatabase(requireContext()).recipeDao()
                val savedRecipes = dao.getSavedRecipesForUser(userId)

                val recipes = savedRecipes.take(3).map {
                    Recipe(
                        title = it.title,
                        description = it.description,
                        imageUrl = it.imageUrl,
                        category = it.category,
                        cuisine = it.cuisine,
                        tags = it.tags,
                        prepTime = it.prepTime,
                        cookingTime = it.cookingTime,
                        totalTime = it.totalTime,
                        servings = it.servings,
                        ingredients = it.ingredients,
                        instructions = it.instructions
                    )
                }

                requireActivity().runOnUiThread {
                    savedRecipesList.clear()
                    savedRecipesList.addAll(recipes)

                    if (recipes.isEmpty()) {
                        binding.noSavedRecipesTextView.visibility = View.VISIBLE
                        binding.noSavedRecipesTextView.text = "No saved recipes yet. Start exploring!"
                        binding.savedRecipesPreviewRecyclerView.visibility = View.GONE
                    } else {
                        binding.noSavedRecipesTextView.visibility = View.GONE
                        binding.savedRecipesPreviewRecyclerView.visibility = View.VISIBLE
                        savedRecipesAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.w("HomeFragment", "Error loading saved recipes: ", e)
                requireActivity().runOnUiThread {
                    binding.noSavedRecipesTextView.visibility = View.VISIBLE
                    binding.noSavedRecipesTextView.text = "Unable to load saved recipes."
                    binding.savedRecipesPreviewRecyclerView.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun loadInventorySummary(userId: String) {
        db.collection("products")
            .whereEqualTo("userId", userId)
            .whereEqualTo("deleted", false)
            .get()
            .addOnSuccessListener { documents ->
                val totalItems = documents.size()
                if (totalItems == 0) {
                    binding.totalItemsTextView.text = "Your inventory is empty. Start by adding some items!"
                } else {
                    val itemText = if (totalItems == 1) "item" else "items"
                    binding.totalItemsTextView.text = "You have $totalItems $itemText in your inventory."
                }
            }
            .addOnFailureListener { exception ->
                Log.w("HomeFragment", "Error loading inventory summary: ", exception)
                binding.totalItemsTextView.text = "Unable to load inventory summary."
            }
    }

    private fun loadRecommendedRecipe() {
        // Load a random recipe from Firestore
        db.collection("recipes")
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val recipes = result.documents
                    val randomRecipe = recipes.random()

                    try {
                        val recipe = randomRecipe.toObject(Recipe::class.java)
                        if (recipe != null) {
                            populateRecommendedRecipe(recipe)
                        } else {
                            showFallbackRecommendedRecipe()
                        }
                    } catch (e: Exception) {
                        Log.w("HomeFragment", "Error parsing random recipe: ", e)
                        showFallbackRecommendedRecipe()
                    }
                } else {
                    // No recipes in database, hide the recommended section
                    binding.recommendedRecipeCardContainer.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                Log.w("HomeFragment", "Error loading random recipe: ", exception)
                showFallbackRecommendedRecipe()
            }
    }

    private fun showFallbackRecommendedRecipe() {
        // Show a static recipe as fallback
        val recommendedRecipe = Recipe(
            title = "Quick Tomato Pasta",
            description = "A delicious and easy pasta dish ready in 20 minutes.",
            imageUrl = "https://images.unsplash.com/photo-1598866594240-a71610fa4646?q=80&w=1920&auto=format&fit=crop",
            category = "Main Course",
            cuisine = "Italian",
            prepTime = 5,
            cookingTime = 15,
            servings = 2,
            dateAdded = Timestamp.now(),
            ingredients = listOf(
                Ingredient("Pasta", 200.0, "g"),
                Ingredient("Tomato Sauce", 1.0, "can")
            ),
            instructions = listOf("Boil pasta.", "Heat sauce.", "Combine and serve.")
        )
        populateRecommendedRecipe(recommendedRecipe)
    }

    private fun populateRecommendedRecipe(recipe: Recipe) {
        val recipeCardBinding = ItemRecipeBinding.inflate(
            layoutInflater,
            binding.recommendedRecipeCardContainer,
            false
        )

        recipeCardBinding.recipeTitle.text = recipe.title
        recipeCardBinding.recipeDescription.text = recipe.description
        recipeCardBinding.recipeInfo.text = "Prep: ${recipe.prepTime} min | Servings: ${recipe.servings}"

        if (recipe.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .centerCrop()
                .into(recipeCardBinding.recipeImage)
        } else {
            recipeCardBinding.recipeImage.setImageResource(R.drawable.ic_placeholder)
        }

        recipeCardBinding.root.setOnClickListener {
            // Toast.makeText(context, "Clicked on recommended: ${recipe.title}", Toast.LENGTH_SHORT).show()
            val detailFragment = RecipeDetailFragment.newInstance(recipe, false)
            navigateToFragment(detailFragment, "RecipeDetailFragment")
        }

        binding.recommendedRecipeCardContainer.removeAllViews()
        binding.recommendedRecipeCardContainer.addView(recipeCardBinding.root)
        binding.recommendedRecipeCardContainer.visibility = View.VISIBLE
    }

    private fun showNotLoggedInState() {
        binding.welcomeMessageTextView.text = "Please log in to view your food tracker."

        // Hide or show appropriate empty states
        binding.noExpiringItemsTextView.visibility = View.VISIBLE
        binding.noExpiringItemsTextView.text = "Please log in to view expiring items."
        binding.expiringProductsRecyclerView.visibility = View.GONE

        binding.noSavedRecipesTextView.visibility = View.VISIBLE
        binding.noSavedRecipesTextView.text = "Please log in to view saved recipes."
        binding.savedRecipesPreviewRecyclerView.visibility = View.GONE

        binding.totalItemsTextView.text = "Please log in to view your inventory."

        // Hide recommended recipe
        binding.recommendedRecipeCardContainer.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        loadRealData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.expiringProductsRecyclerView.adapter = null
        binding.savedRecipesPreviewRecyclerView.adapter = null
        _binding = null
    }
}