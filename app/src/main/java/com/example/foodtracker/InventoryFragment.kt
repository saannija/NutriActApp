package com.example.foodtracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.model.InventoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class InventoryFragment : Fragment() {

    private lateinit var inventoryRecyclerView: RecyclerView
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val inventoryList = mutableListOf<InventoryItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        inventoryRecyclerView = view.findViewById(R.id.inventoryRecyclerView)
        inventoryRecyclerView.layoutManager = LinearLayoutManager(context)

        inventoryAdapter = InventoryAdapter(inventoryList) { item -> // Pass click listener
            editInventoryItem(item)
        }
        inventoryRecyclerView.adapter = inventoryAdapter

        loadInventoryData()

        return view
    }

    private fun loadInventoryData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "User not logged in.")
            return
        }

        db.collection("products")
            .whereEqualTo("userId", userId)
            .orderBy("expirationDate", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                inventoryList.clear()
                for (document in documents) {
                    val productName = document.getString("productName") ?: ""
                    val expirationDate = document.getTimestamp("expirationDate")
                    val category = document.getString("category")
                    val iconResId = getIconForCategory(category)
                    val documentId = document.id

                    val inventoryItem = InventoryItem(
                        productName,
                        expirationDate,
                        category,
                        iconResId,
                        documentId
                    )
                    inventoryList.add(inventoryItem)
                }
                inventoryAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    private fun getIconForCategory(category: String?): Int {
        return when (category?.lowercase(Locale.ENGLISH)) {
            "vegetable", "vegetables" -> R.drawable.ic_vegetable
            "fruit", "fruits" -> R.drawable.ic_fruit
            "dairy", "milk", "cheese", "yogurt" -> R.drawable.ic_dairy
            "meat", "beef", "chicken", "pork" -> R.drawable.ic_meat
            "grain", "bread", "rice", "pasta" -> R.drawable.ic_grain
            "canned goods", "canned" -> R.drawable.ic_canned_goods
            "beverage", "drinks" -> R.drawable.ic_beverage
            else -> R.drawable.ic_default
        }
    }

    private fun editInventoryItem(item: InventoryItem) {
        val bundle = Bundle().apply {
            putString("documentId", item.documentId)
            putString("productName", item.productName)
            putString("category", item.category)
            putLong("expirationDate", item.expirationDate?.toDate()?.time ?: 0)
            // Add other fields as needed...
            putBoolean("hasScannedData", true) // Indicate that we're in "edit" mode and should show manual entry
        }
        val addFragment = AddFragment().apply {
            arguments = bundle
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, addFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        loadInventoryData()
    }

    companion object {
        private const val TAG = "InventoryFragment"
    }
}