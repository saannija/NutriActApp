package com.example.foodtracker.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.R
import com.example.foodtracker.model.AppNotification
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private val notifications: MutableList<AppNotification>,
    private val onNotificationClick: (AppNotification) -> Unit,
    private val onDeleteClick: (AppNotification, Int) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.notificationTitle)
        val messageTextView: TextView = itemView.findViewById(R.id.notificationMessage)
        val timeTextView: TextView = itemView.findViewById(R.id.notificationTime)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        val expandedLayout: LinearLayout = itemView.findViewById(R.id.expandedLayout)
        val expiryDateTextView: TextView = itemView.findViewById(R.id.expiryDateTextView)
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val notificationCard: LinearLayout = itemView.findViewById(R.id.notificationCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        val isExpanded = expandedPositions.contains(position)

        holder.titleTextView.text = notification.title
        holder.messageTextView.text = notification.message

        // Format time
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.timeTextView.text = dateFormat.format(notification.createdAt.toDate())

        // Set visual state based on read status
        if (notification.isRead) {
            // Read state - gray background and muted text colors
            holder.notificationCard.setBackgroundColor(Color.parseColor("#F5F5F5"))
            holder.titleTextView.setTextColor(Color.parseColor("#888888"))
            holder.messageTextView.setTextColor(Color.parseColor("#888888"))
            holder.timeTextView.setTextColor(Color.parseColor("#AAAAAA"))
            holder.productNameTextView.setTextColor(Color.parseColor("#888888"))
            holder.expiryDateTextView.setTextColor(Color.parseColor("#888888"))
        } else {
            // Unread state - normal colors
            holder.notificationCard.setBackgroundColor(Color.WHITE)
            holder.titleTextView.setTextColor(Color.parseColor("#000000"))
            holder.messageTextView.setTextColor(Color.parseColor("#000000"))
            holder.timeTextView.setTextColor(Color.parseColor("#666666"))
            holder.productNameTextView.setTextColor(Color.parseColor("#000000"))
            holder.expiryDateTextView.setTextColor(Color.parseColor("#000000"))
        }

        // Handle expanded state
        if (isExpanded) {
            holder.expandedLayout.visibility = View.VISIBLE
            holder.productNameTextView.text = "Product: ${notification.productName}"

            notification.expirationDate?.let { expiryDate ->
                val expiryFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                holder.expiryDateTextView.text = "Expires: ${expiryFormat.format(expiryDate.toDate())}"
            }
        } else {
            holder.expandedLayout.visibility = View.GONE
        }

        // Click listeners
        holder.itemView.setOnClickListener {
            if (isExpanded) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)
            onNotificationClick(notification)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(notification, position)
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun removeNotification(position: Int) {
        if (position in 0 until notifications.size) {
            notifications.removeAt(position)
            expandedPositions.remove(position)
            // Adjust expanded positions after removal
            expandedPositions.removeAll { it > position }
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, notifications.size)
        }
    }

    fun updateNotifications(newNotifications: List<AppNotification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        expandedPositions.clear()
        notifyDataSetChanged()
    }
}