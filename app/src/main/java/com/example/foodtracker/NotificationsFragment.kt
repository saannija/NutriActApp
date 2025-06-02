package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

// import androidx.recyclerview.widget.LinearLayoutManager
// import androidx.recyclerview.widget.RecyclerView

class NotificationsFragment : Fragment() {

    private lateinit var noNotificationsTextView: TextView
    // private lateinit var notificationsRecyclerView: RecyclerView
    // private lateinit var notificationAdapter: YourNotificationAdapter

    // Placeholder
    private val notificationsList = mutableListOf<Any>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        noNotificationsTextView = view.findViewById(R.id.noNotificationsTextView)
        // notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView)

        // setupRecyclerView()
        loadNotifications()

        return view
    }

    /* // For later
    private fun setupRecyclerView() {
        // notificationAdapter = YourNotificationAdapter(notificationsList)
        // notificationsRecyclerView.layoutManager = LinearLayoutManager(context)
        // notificationsRecyclerView.adapter = notificationAdapter
    }
    */

    private fun loadNotifications() {
        // TODO: Implement logic to fetch actual notifications
        notificationsList.clear()

        updateUI()
    }

    private fun updateUI() {
        if (notificationsList.isEmpty()) {
            noNotificationsTextView.visibility = View.VISIBLE
            // notificationsRecyclerView.visibility = View.GONE
        } else {
            noNotificationsTextView.visibility = View.GONE
            // notificationsRecyclerView.visibility = View.VISIBLE
            // notificationAdapter.notifyDataSetChanged()
        }
    }
}