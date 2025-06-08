package com.example.foodtracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.foodtracker.MainActivity
import com.example.foodtracker.R

class AppNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "food_expiry_channel"
        const val CHANNEL_NAME = "Food Expiry Notifications"
        const val CHANNEL_DESCRIPTION = "Notifications for products about to expire"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showExpiryNotification(
        productName: String,
        daysUntilExpiry: Int,
        productId: String
    ) {
        val title = if (daysUntilExpiry < 0) {
            "Product Expired!"
        } else {
            "Product Expiring Soon"
        }

        val message = when (daysUntilExpiry) {
            0 -> "$productName expires today"
            1 -> "$productName expires tomorrow"
            else -> if (daysUntilExpiry < 0) {
                "$productName has expired"
            } else {
                "$productName expires in $daysUntilExpiry days"
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            productId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_BASE + productId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Handle permission denied
            e.printStackTrace()
        }
    }
}