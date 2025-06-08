package com.example.foodtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.adapters.NotificationAdapter
import com.example.foodtracker.model.AppNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsFragment : Fragment() {

    private lateinit var noNotificationsTextView: TextView
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationsList = mutableListOf<AppNotification>()

    // Permission launcher for notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        noNotificationsTextView = view.findViewById(R.id.noNotificationsTextView)
        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView)

        setupRecyclerView()
        checkNotificationPermission()
        loadNotifications()

        return view
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notificationsList,
            onNotificationClick = { notification ->
                markAsRead(notification)
            },
            onDeleteClick = { notification, position ->
                deleteNotification(notification, position)
            }
        )
        notificationsRecyclerView.layoutManager = LinearLayoutManager(context)
        notificationsRecyclerView.adapter = notificationAdapter
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Request permission
                    Toast.makeText(
                        context,
                        "Notification permission needed for expiry alerts",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission directly
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun loadNotifications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            updateUI()
            return
        }

        firestore.collection("notifications")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("NotificationsFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val notifications = snapshots.toObjects(AppNotification::class.java)
                    notificationAdapter.updateNotifications(notifications)
                    updateUI()
                }
            }
    }

    private fun markAsRead(notification: AppNotification) {
        if (!notification.isRead) {
            firestore.collection("notifications")
                .document(notification.id)
                .update("isRead", true)
                .addOnSuccessListener {
                    Log.d("NotificationsFragment", "Notification marked as read")
                }
                .addOnFailureListener { e ->
                    Log.w("NotificationsFragment", "Error marking notification as read", e)
                }
        }
    }

    private fun deleteNotification(notification: AppNotification, position: Int) {
        firestore.collection("notifications")
            .document(notification.id)
            .delete()
            .addOnSuccessListener {
                notificationAdapter.removeNotification(position)
                updateUI()
                Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("NotificationsFragment", "Error deleting notification", e)
                Toast.makeText(context, "Failed to delete notification", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        if (notificationsList.isEmpty()) {
            noNotificationsTextView.visibility = View.VISIBLE
            notificationsRecyclerView.visibility = View.GONE
        } else {
            noNotificationsTextView.visibility = View.GONE
            notificationsRecyclerView.visibility = View.VISIBLE
        }
    }
}