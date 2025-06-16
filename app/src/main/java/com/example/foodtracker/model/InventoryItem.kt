package com.example.foodtracker.model

import com.example.foodtracker.R
import com.google.firebase.Timestamp
import java.util.Date

data class InventoryItem(
    val productName: String,
    val expirationDate: Timestamp?,
    val category: String? = null,
    var iconResId: Int = R.drawable.ic_default,
    val documentId: String,
    val deleted: Boolean = false,
    val type: String? = null,
    val storageStatus: String? = null,
    val quantity: Int = 0,
    val unit: String? = null,
    val totalAmount: Int = 0,
    val notes: String? = null,
    val allergenAlert: Boolean = false,
    val openedDate: Timestamp? = null
) {
    // Helper function to get effective expiration date
    fun getEffectiveExpirationDate(): Timestamp? {
        return if (storageStatus == "Opened" && openedDate != null) {
            // 3 days after opening
            val openedExpiryMillis = openedDate.toDate().time + (3 * 24 * 60 * 60 * 1000L)
            val openedExpiryDate = Timestamp(Date(openedExpiryMillis))

            // Return earlier of original expiry or opened expiry
            if (expirationDate != null && expirationDate.toDate().before(openedExpiryDate.toDate())) {
                expirationDate
            } else {
                openedExpiryDate
            }
        } else {
            expirationDate
        }
    }
}