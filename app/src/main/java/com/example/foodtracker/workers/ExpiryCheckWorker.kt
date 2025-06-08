package com.example.foodtracker.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodtracker.model.AppNotification
import com.example.foodtracker.model.Product
import com.example.foodtracker.util.AppNotificationManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ExpiryCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationManager = AppNotificationManager(context)

    override suspend fun doWork(): Result {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.d("ExpiryCheckWorker", "User not authenticated")
                return Result.success()
            }

            checkForExpiringProducts(currentUser.uid)
            Result.success()
        } catch (exception: Exception) {
            Log.e("ExpiryCheckWorker", "Error checking expiry", exception)
            Result.retry()
        }
    }

    private suspend fun checkForExpiringProducts(userId: String) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val threeDaysFromNow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 3)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Get products that expire within 3 days
        val products = firestore.collection("products")
            .whereEqualTo("userId", userId)
            .whereEqualTo("deleted", false)
            .get()
            .await()
            .toObjects(Product::class.java)

        val expiringProducts = products.filter { product ->
            product.expirationDate?.let { expiryDate ->
                val expiryCalendar = Calendar.getInstance().apply {
                    time = expiryDate.toDate()
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                // Check if expiry date is today or within next 3 days
                !expiryCalendar.before(today) && !expiryCalendar.after(threeDaysFromNow)
            } ?: false
        }

        for (product in expiringProducts) {
            val daysUntilExpiry = calculateDaysUntilExpiry(product.expirationDate!!)

            // Problems here:
            val existingNotification = checkExistingNotification(userId, product.id, daysUntilExpiry)
            val hasUnreadNotifications = checkUnreadNotifications(userId, product.id)

            // Only send notification if:
            // 1. No existing notification for this product+days combination today
            // 2. No unread notifications exist for this product
            if (existingNotification == null && !hasUnreadNotifications) {
                // Create notification in Firestore
                saveNotificationToFirestore(userId, product, daysUntilExpiry)

                // Show system notification
                notificationManager.showExpiryNotification(
                    product.productName,
                    daysUntilExpiry,
                    product.id
                )
            }
        }
    }

    private suspend fun checkExistingNotification(
        userId: String,
        productId: String,
        daysUntilExpiry: Int
    ): AppNotification? {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return try {
            val notifications = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("productId", productId)
                .whereEqualTo("daysUntilExpiry", daysUntilExpiry)
                .whereGreaterThanOrEqualTo("createdAt", Timestamp(startOfDay.time))
                .get()
                .await()
                .toObjects(AppNotification::class.java)

            notifications.firstOrNull()
        } catch (e: Exception) {
            Log.e("ExpiryCheckWorker", "Error checking existing notification", e)
            null
        }
    }

    private suspend fun checkUnreadNotifications(
        userId: String,
        productId: String
    ): Boolean {
        return try {
            val unreadNotifications = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("productId", productId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            !unreadNotifications.isEmpty
        } catch (e: Exception) {
            Log.e("ExpiryCheckWorker", "Error checking unread notifications", e)
            false
        }
    }

    private suspend fun saveNotificationToFirestore(
        userId: String,
        product: Product,
        daysUntilExpiry: Int
    ) {
        val title = if (daysUntilExpiry <= 0) {
            "Product Expired!"
        } else {
            "Product Expiring Soon"
        }

        val message = when (daysUntilExpiry) {
            0 -> "${product.productName} expires today"
            1 -> "${product.productName} expires tomorrow"
            else -> if (daysUntilExpiry < 0) {
                "${product.productName} has expired"
            } else {
                "${product.productName} expires in $daysUntilExpiry days"
            }
        }

        val notification = AppNotification(
            userId = userId,
            productId = product.id,
            productName = product.productName,
            expirationDate = product.expirationDate,
            title = title,
            message = message,
            type = if (daysUntilExpiry < 0) "EXPIRED" else "EXPIRY_WARNING",
            daysUntilExpiry = daysUntilExpiry
        )

        try {
            firestore.collection("notifications")
                .add(notification)
                .await()
        } catch (e: Exception) {
            Log.e("ExpiryCheckWorker", "Error saving notification", e)
        }
    }

    private fun calculateDaysUntilExpiry(expirationDate: Timestamp): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val expiryDate = Calendar.getInstance().apply {
            time = expirationDate.toDate()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = expiryDate.timeInMillis - today.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}