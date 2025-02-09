package com.example.foodtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.model.InventoryItem
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class InventoryAdapter(private val inventoryList: List<InventoryItem>) :
    RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val expirationDateTextView: TextView = itemView.findViewById(R.id.expirationDateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.inventory_item, parent, false)
        return InventoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val currentItem = inventoryList[position]
        holder.productNameTextView.text = currentItem.productName
        holder.expirationDateTextView.text =
            formatTimestamp(currentItem.expirationDate)
    }

    override fun getItemCount() = inventoryList.size

    private fun formatTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) {
            return "No Expiration Date"
        }
        val date = timestamp.toDate()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return dateFormat.format(date)
    }
}