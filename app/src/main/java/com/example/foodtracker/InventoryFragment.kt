package com.example.foodtracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.model.InventoryItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class InventoryFragment : Fragment() {

    private lateinit var inventoryRecyclerView: RecyclerView
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var emptyStateTextView: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val inventoryList = mutableListOf<InventoryItem>()

    private val nearingExpirationThresholdDays = 3

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        inventoryRecyclerView = view.findViewById(R.id.inventoryRecyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)

        inventoryRecyclerView.layoutManager = LinearLayoutManager(context)

        inventoryAdapter = InventoryAdapter(
            inventoryList,
            { item -> editInventoryItem(item) },
            { item -> showDeleteConfirmationDialog(item)  }
        )
        inventoryRecyclerView.adapter = inventoryAdapter

        loadInventoryData()

        return view
    }

    private fun updateEmptyState() {
        if (inventoryList.isEmpty()) {
            inventoryRecyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.VISIBLE
        } else {
            inventoryRecyclerView.visibility = View.VISIBLE
            emptyStateTextView.visibility = View.GONE
        }
    }

    private fun loadInventoryData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "User not logged in.")
            return
        }

        val combinedFetchedItems = mutableListOf<InventoryItem>()

        // Query 1: Get items where deleted is false
        db.collection("products")
            .whereEqualTo("userId", userId)
            .whereEqualTo("deleted", false)
            .orderBy("expirationDate", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val inventoryItem = documentToInventoryItem(document)
                    combinedFetchedItems.add(inventoryItem)
                }
                // After Query 1 completes, execute Query 2
                queryItemsThatMightMissDeletedField(userId, combinedFetchedItems)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents (Query 1): ", exception)
                // Still update empty state even if query fails
                updateEmptyState()
            }
    }

    private fun calculateDaysRemaining(expirationTimestamp: Timestamp?): Long {
        if (expirationTimestamp == null) {
            return Long.MAX_VALUE
        }
        val expirationDate = expirationTimestamp.toDate()
        val nowCal = Calendar.getInstance()
        val expirationCal = Calendar.getInstance().apply { time = expirationDate }

        nowCal.set(Calendar.HOUR_OF_DAY, 0); nowCal.set(Calendar.MINUTE, 0); nowCal.set(Calendar.SECOND, 0); nowCal.set(Calendar.MILLISECOND, 0)
        expirationCal.set(Calendar.HOUR_OF_DAY, 0); expirationCal.set(Calendar.MINUTE, 0); expirationCal.set(Calendar.SECOND, 0); expirationCal.set(Calendar.MILLISECOND, 0)

        val diffMillis = expirationCal.timeInMillis - nowCal.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    private fun determineItemExpirationStatus(item: InventoryItem, thresholdDays: Int): ExpirationStatus {
        if (item.expirationDate == null) {
            return ExpirationStatus.NO_DATE
        }
        val daysRemaining = calculateDaysRemaining(item.expirationDate)

        return when {
            daysRemaining < 0 -> ExpirationStatus.EXPIRED
            daysRemaining < thresholdDays -> ExpirationStatus.NEARING_EXPIRATION
            else -> ExpirationStatus.NORMAL
        }
    }

    private fun queryItemsThatMightMissDeletedField(userId: String, currentItems: MutableList<InventoryItem>) {
        // Query 2: Get all items for the user and filter in the app
        db.collection("products")
            .whereEqualTo("userId", userId)
            .orderBy("expirationDate", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (currentItems.none { it.documentId == document.id }) {
                        val deleted = document.getBoolean("deleted") ?: false
                        if (!deleted) {
                            val inventoryItem = documentToInventoryItem(document)
                            currentItems.add(inventoryItem)
                        }
                    }
                }

                currentItems.sortWith(
                    compareBy<InventoryItem> { item ->
                        val status = determineItemExpirationStatus(item, nearingExpirationThresholdDays)
                        when (status) {
                            ExpirationStatus.EXPIRED -> 0
                            ExpirationStatus.NEARING_EXPIRATION -> 1
                            ExpirationStatus.NORMAL -> 2
                            ExpirationStatus.NO_DATE -> 3
                        }
                    }.thenBy { item ->
                        item.expirationDate?.toDate()?.time ?: Long.MAX_VALUE
                    }
                )

                inventoryList.clear()
                inventoryList.addAll(currentItems)
                inventoryAdapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents (Query 2): ", exception)
                updateEmptyState()
            }
    }

    private fun documentToInventoryItem(document: DocumentSnapshot): InventoryItem {
        val productName = document.getString("productName") ?: ""
        val expirationDate = document.getTimestamp("expirationDate")
        val category = document.getString("category")
        val iconResId = getIconForCategory(category)
        val documentId = document.id
        val deleted = document.getBoolean("deleted") ?: false
        val type = document.getString("type")
        val storageStatus = document.getString("storageStatus")
        val quantity = document.getLong("quantity")?.toInt() ?: 0
        val unit = document.getString("unit")
        val totalAmount = document.getLong("totalAmount")?.toInt() ?: 0
        val notes = document.getString("notes") ?: ""
        val allergenAlert = document.getBoolean("allergenAlert") ?: false


        return InventoryItem(
            productName,
            expirationDate,
            category,
            iconResId,
            documentId,
            deleted,
            type,
            storageStatus,
            quantity,
            unit,
            totalAmount,
            notes,
            allergenAlert
        )
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
            putString("type", item.type)
            putString("storageStatus", item.storageStatus)
            putInt("quantity", item.quantity)
            putString("unit", item.unit)
            putInt("totalAmount", item.totalAmount)
            putString("notes", item.notes)
            putBoolean("allergenAlert", item.allergenAlert)

            putBoolean("hasScannedData", true)
        }
        val addFragment = AddFragment().apply {
            arguments = bundle
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, addFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showDeleteConfirmationDialog(item: InventoryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Do you really want to remove ${item.productName}?")
            .setPositiveButton("Delete") { dialog, which ->
                // User clicked "Delete", proceed with the actual deletion
                performDeleteInventoryItem(item)
            }
            .setNegativeButton("Cancel", null) // User clicked "Cancel", do nothing
            .setIcon(R.drawable.ic_delete)
            .show()
    }

    private fun performDeleteInventoryItem(item: InventoryItem) {
        db.collection("products")
            .document(item.documentId)
            .update("deleted", true)
            .addOnSuccessListener {
                val itemIndex = inventoryList.indexOfFirst { it.documentId == item.documentId }
                if (itemIndex != -1) {
                    inventoryList.removeAt(itemIndex)
                    inventoryAdapter.notifyItemRemoved(itemIndex)
                    updateEmptyState()
                } else {
                    // If not found (shouldn't happen)
                    loadInventoryData()
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error deleting item: ", e)
            }
    }

    override fun onResume() {
        super.onResume()
        loadInventoryData()
    }

    companion object {
        private const val TAG = "InventoryFragment"
    }
}