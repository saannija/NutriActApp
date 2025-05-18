package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.foodtracker.databinding.FragmentHomeBinding
import com.example.foodtracker.databinding.ItemRecipeBinding
import com.example.foodtracker.model.Ingredient
import com.example.foodtracker.model.Product
import com.example.foodtracker.model.Recipe
import com.google.firebase.Timestamp
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var expiringProductsAdapter: ExpiringProductsAdapter
    private lateinit var savedRecipesAdapter: RecipeAdapter

    // Dummy lists for the adapters
    private var dummySavedRecipeList: MutableList<Recipe> = mutableListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        setupAdapters()
        setupClickListeners()
        setupRecyclerViews()
        loadDummyData()

        return view
    }

    private fun setupAdapters() {
        expiringProductsAdapter = ExpiringProductsAdapter { product ->
            Toast.makeText(context, "Clicked on expiring: ${product.productName}", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to product detail screen or edit screen
        }

        // Initialize your RecipeAdapter for the saved recipes preview
        savedRecipesAdapter = RecipeAdapter(dummySavedRecipeList) { recipe -> // Pass the list and click listener
            Toast.makeText(context, "Clicked on saved recipe: ${recipe.title}", Toast.LENGTH_SHORT).show()
            val detailFragment = RecipeDetailFragment.newInstance(recipe, true)
            navigateToFragment(detailFragment, "RecipeDetailFragment")
        }
    }

    private fun setupClickListeners() {
        binding.quickAddScanButton.setOnClickListener {
            Toast.makeText(context, "Scan Item Clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement navigation or action for Scan Item
        }

        binding.quickAddManualButton.setOnClickListener {
            Toast.makeText(context, "Add Manually Clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement navigation or action for Add Manually
        }

        binding.viewAllSavedRecipesButton.setOnClickListener {
            navigateToFragment(SavedRecipesFragment(), "SavedRecipesFragment")
        }

        binding.inventorySummaryCard.setOnClickListener {
            Toast.makeText(context, "View Full Inventory Clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement navigation to Full Inventory screen
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
            Toast.makeText(context, "Clicked on recommended: ${recipe.title}", Toast.LENGTH_SHORT).show()
            val detailFragment = RecipeDetailFragment.newInstance(recipe, false)
            navigateToFragment(detailFragment, "RecipeDetailFragment")
        }

        binding.recommendedRecipeCardContainer.removeAllViews()
        binding.recommendedRecipeCardContainer.addView(recipeCardBinding.root)
        binding.recommendedRecipeCardContainer.visibility = View.VISIBLE
    }


    private fun loadDummyData() {
        binding.welcomeMessageTextView.text = "Welcome back, Foodie!"

        val recommendedRecipe = Recipe(
            title = "Quick Tomato Pasta",
            description = "A delicious and easy pasta dish ready in 20 minutes.",
            imageUrl = "https://images.unsplash.com/photo-1598866594240-a71610fa4646?q=80&w=1920&auto=format&fit=crop",
            category = "Main Course", cuisine = "Italian", prepTime = 5, cookingTime = 15, servings = 2,
            dateAdded = Timestamp.now(),
            ingredients = listOf(Ingredient("Pasta", 200.0, "g"), Ingredient("Tomato Sauce", 1.0, "can")),
            instructions = listOf("Boil pasta.", "Heat sauce.", "Combine and serve.")
        )
        populateRecommendedRecipe(recommendedRecipe)

        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 2); val twoDaysFromNow = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 3); val fiveDaysFromNow = calendar.time
        calendar.time = today; calendar.add(Calendar.DAY_OF_YEAR, -1); val yesterday = calendar.time

        val dummyProducts = listOf(
            Product(id="1", productName = "Milk", category = "Dairy", expirationDate = Timestamp(twoDaysFromNow), quantity = 1, unit = "Litre"),
            Product(id="2", productName = "Eggs", category = "Dairy", expirationDate = Timestamp(fiveDaysFromNow), quantity = 12, unit = "pcs"),
            Product(id="3", productName = "Bread", category = "Bakery", expirationDate = Timestamp(yesterday), quantity = 1, unit = "loaf"),
            Product(id="4", productName = "Chicken Breast", category = "Meat", expirationDate = Timestamp(twoDaysFromNow), quantity = 2, unit = "pcs"),
            Product(id="5", productName = "Apples", category = "Fruit", expirationDate = Timestamp(fiveDaysFromNow), quantity = 5, unit = "pcs")
        )
        if (dummyProducts.isEmpty()) {
            binding.noExpiringItemsTextView.visibility = View.VISIBLE
            binding.expiringProductsRecyclerView.visibility = View.GONE
        } else {
            binding.noExpiringItemsTextView.visibility = View.GONE
            binding.expiringProductsRecyclerView.visibility = View.VISIBLE
            expiringProductsAdapter.submitList(dummyProducts)
        }

        // Clear previous dummy data and add new
        dummySavedRecipeList.clear()
        dummySavedRecipeList.addAll(listOf(
            Recipe(title = "Classic Pancakes", description = "Fluffy breakfast pancakes for the whole family.", imageUrl = "https://images.unsplash.com/photo-1528207776546-365bb710ee93?q=80&w=2070&auto=format&fit=crop", servings = 4, prepTime = 10, cookingTime = 15),
            Recipe(title = "Grilled Chicken Salad", description = "Healthy and quick salad with grilled chicken.", imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?q=80&w=2070&auto=format&fit=crop", servings = 2, prepTime = 15, cookingTime = 10),
            Recipe(title = "Berry Smoothie Bowl", description = "Nutritious and vibrant smoothie bowl to start your day.", imageUrl = "https://images.unsplash.com/photo-1549339120-16004547059b?q=80&w=2070&auto=format&fit=crop", servings = 1, prepTime = 5, cookingTime = 0)
        ))

        if (dummySavedRecipeList.isEmpty()) {
            binding.noSavedRecipesTextView.visibility = View.VISIBLE
            binding.savedRecipesPreviewRecyclerView.visibility = View.GONE
        } else {
            binding.noSavedRecipesTextView.visibility = View.GONE
            binding.savedRecipesPreviewRecyclerView.visibility = View.VISIBLE
            savedRecipesAdapter.notifyDataSetChanged()
        }

        // 5. Inventory Summary
        val totalItemsInInventory = 37
        binding.totalItemsTextView.text = "You have $totalItemsInInventory items in your inventory."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.expiringProductsRecyclerView.adapter = null
        binding.savedRecipesPreviewRecyclerView.adapter = null
        _binding = null
    }
}