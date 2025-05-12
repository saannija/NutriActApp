
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.AddFragment
import com.example.foodtracker.R
import com.example.foodtracker.model.InventoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

        inventoryAdapter = InventoryAdapter(
            inventoryList,
            { item -> editInventoryItem(item) },
            { item -> deleteInventoryItem(item) }
        )
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

        val inventoryItems = mutableListOf<InventoryItem>()

        // Query 1: Get items where deleted is false
        db.collection("products")
            .whereEqualTo("userId", userId)
            .whereEqualTo("deleted", false)
            .orderBy("expirationDate", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val inventoryItem = documentToInventoryItem(document)
                    inventoryItems.add(inventoryItem)
                }
                // After Query 1 completes, execute Query 2
                queryItemsWithoutDeletedField(userId, inventoryItems)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents (Query 1): ", exception)
            }
    }

    private fun queryItemsWithoutDeletedField(userId: String, existingItems: MutableList<InventoryItem>) {
        // Query 2: Get all items for the user and filter in the app
        db.collection("products")
            .whereEqualTo("userId", userId)
            .orderBy("expirationDate", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (existingItems.none { it.documentId == document.id }) {
                        val deleted = document.getBoolean("deleted")
                        if (deleted != true) {
                            val inventoryItem = documentToInventoryItem(document)
                            existingItems.add(inventoryItem)
                        }
                    }
                }

                inventoryList.clear()
                inventoryList.addAll(existingItems)
                inventoryAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents (Query 2): ", exception)
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
        val notes = document.getString("notes")
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
            allergenAlert,
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

            putBoolean("hasScannedData", true) // Set this to true to show the manual entry form
        }
        val addFragment = AddFragment().apply {
            arguments = bundle
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, addFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteInventoryItem(item: InventoryItem) {
        db.collection("products")
            .document(item.documentId)
            .update("deleted", true)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully soft deleted!")
                loadInventoryData()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error soft deleting document", e)
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