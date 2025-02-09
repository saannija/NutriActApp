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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Date

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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Find the RecyclerView
        inventoryRecyclerView = view.findViewById(R.id.inventoryRecyclerView)
        inventoryRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter
        inventoryAdapter = InventoryAdapter(inventoryList)
        inventoryRecyclerView.adapter = inventoryAdapter

        // Load data from Firebase
        loadInventoryData()

        return view
    }

    private fun loadInventoryData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {Log.w(TAG, "User not logged in.")
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
                    val inventoryItem = InventoryItem(productName, expirationDate)
                    inventoryList.add(inventoryItem)
                }
                inventoryAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    companion object {
        private const val TAG = "InventoryFragment"
    }
}