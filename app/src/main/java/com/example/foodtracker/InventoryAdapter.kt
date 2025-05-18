package com.example.foodtracker

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.model.InventoryItem
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// Enum to represent expiration status for clarity
enum class ExpirationStatus {
    NORMAL, NEARING_EXPIRATION, EXPIRED, NO_DATE
}

class InventoryAdapter(
    private val inventoryList: List<InventoryItem>,
    private val onItemClick: (InventoryItem) -> Unit,
    private val onDeleteClick: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    private val nearingExpirationThresholdDays = 3

    class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        private val expirationDateTextView: TextView = itemView.findViewById(R.id.expirationDateTextView)
        private val categoryIconImageView: ImageView = itemView.findViewById(R.id.categoryIconImageView)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.inventoryItemCardView)
        private val deleteIconImageView: ImageView = itemView.findViewById(R.id.deleteIconImageView)
        private val context = itemView.context

        fun bind(
            item: InventoryItem,
            onItemClick: (InventoryItem) -> Unit,
            onDeleteClick: (InventoryItem) -> Unit,
            nearingThreshold: Int // Pass threshold from adapter
        ) {
            productNameTextView.text = item.productName
            if (item.iconResId != 0 && item.iconResId != null) { // Check for non-zero and non-null
                categoryIconImageView.setImageResource(item.iconResId)
            } else {
                categoryIconImageView.setImageResource(R.drawable.ic_default) // Fallback
            }

            //Set content descriptions
            categoryIconImageView.contentDescription = "Category icon for ${item.category ?: "item"}"
            deleteIconImageView.contentDescription = "Delete ${item.productName ?: "item"}"
            productNameTextView.contentDescription = "Product name: ${item.productName ?: "Unnamed product"}"
            expirationDateTextView.contentDescription = getExpirationContentDescription(item.expirationDate, nearingThreshold)


            cardView.setOnClickListener { onItemClick(item) }
            deleteIconImageView.setOnClickListener { onDeleteClick(item) }

            val expirationStatus = determineExpirationStatus(item.expirationDate, nearingThreshold)
            updateExpirationAppearance(expirationStatus, item.expirationDate, nearingThreshold)
        }

        private fun getExpirationContentDescription(timestamp: Timestamp?, nearingThreshold: Int): String {
            if (timestamp == null) return "No expiration date"
            val status = determineExpirationStatus(timestamp, nearingThreshold)
            val formattedDate = formatTimestampForDisplay(timestamp.toDate())
            return when (status) {
                ExpirationStatus.EXPIRED -> "Expired on $formattedDate"
                ExpirationStatus.NEARING_EXPIRATION -> "Expires soon, on $formattedDate"
                else -> "Expires on $formattedDate"
            }
        }


        private fun updateExpirationAppearance(status: ExpirationStatus, timestamp: Timestamp?, nearingThreshold: Int) {
            var dateText: String
            val dateTextColor: Int
            var cardStrokeColor: Int

            val defaultTextColorTypedValue = TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, defaultTextColorTypedValue, true)
            val defaultTextColor = defaultTextColorTypedValue.data
            val defaultCardStrokeColor = Color.TRANSPARENT


            when (status) {
                ExpirationStatus.EXPIRED -> {
                    dateText = if (timestamp != null) "Expired: ${formatTimestampForDisplay(timestamp.toDate())}" else "Expired"
                    val errorColorTypedValue = TypedValue()
                    context.theme.resolveAttribute(com.google.android.material.R.attr.colorError, errorColorTypedValue, true)
                    dateTextColor = errorColorTypedValue.data
                    cardStrokeColor = ContextCompat.getColor(context, R.color.error_stroke_red)
                }
                ExpirationStatus.NEARING_EXPIRATION -> {
                    val daysRemaining = if(timestamp != null) calculateDaysRemaining(timestamp.toDate()) else nearingThreshold.toLong()
                    dateText = if (timestamp != null) {
                        when {
                            daysRemaining > 1 -> "Expires in $daysRemaining days"
                            daysRemaining == 1L -> "Expires Tomorrow"
                            daysRemaining == 0L -> "Expires Today"
                            else -> "Expired"
                        }
                    } else "Expires Soon"
                    dateTextColor = ContextCompat.getColor(context, R.color.warning_text_orange)
                    cardStrokeColor = ContextCompat.getColor(context, R.color.warning_stroke_orange)
                }
                ExpirationStatus.NORMAL -> {
                    dateText = if (timestamp != null) "Expires: ${formatTimestampForDisplay(timestamp.toDate())}" else "No Date"
                    dateTextColor = defaultTextColor
                    cardStrokeColor = defaultCardStrokeColor
                }
                ExpirationStatus.NO_DATE -> {
                    dateText = "No Expiration Date"
                    dateTextColor = defaultTextColor
                    cardStrokeColor = defaultCardStrokeColor
                }
            }

            expirationDateTextView.text = dateText
            expirationDateTextView.setTextColor(dateTextColor)
            cardView.strokeColor = cardStrokeColor
        }

        private fun formatTimestampForDisplay(date: java.util.Date): String {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return dateFormat.format(date)
        }

        private fun calculateDaysRemaining(expirationDate: java.util.Date): Long {
            val nowCal = Calendar.getInstance()
            val expirationCal = Calendar.getInstance().apply { time = expirationDate }

            nowCal.set(Calendar.HOUR_OF_DAY, 0); nowCal.set(Calendar.MINUTE, 0); nowCal.set(Calendar.SECOND, 0); nowCal.set(Calendar.MILLISECOND, 0)
            expirationCal.set(Calendar.HOUR_OF_DAY, 0); expirationCal.set(Calendar.MINUTE, 0); expirationCal.set(Calendar.SECOND, 0); expirationCal.set(Calendar.MILLISECOND, 0)

            val diffMillis = expirationCal.timeInMillis - nowCal.timeInMillis
            return TimeUnit.MILLISECONDS.toDays(diffMillis)
        }

        private fun determineExpirationStatus(timestamp: Timestamp?, nearingThresholdDays: Int): ExpirationStatus {
            if (timestamp == null) {
                return ExpirationStatus.NO_DATE
            }
            val daysRemaining = calculateDaysRemaining(timestamp.toDate())

            return when {
                daysRemaining < 0 -> ExpirationStatus.EXPIRED
                daysRemaining < nearingThresholdDays -> ExpirationStatus.NEARING_EXPIRATION
                else -> ExpirationStatus.NORMAL
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory, parent, false)
        return InventoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val currentItem = inventoryList[position]
        holder.bind(currentItem, onItemClick, onDeleteClick, nearingExpirationThresholdDays) // Pass threshold
    }

    override fun getItemCount() = inventoryList.size
}