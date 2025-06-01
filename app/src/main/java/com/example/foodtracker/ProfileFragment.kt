package com.example.foodtracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ProfileFragment : Fragment() {

    private lateinit var btnLogout: Button
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Find the views
        btnLogout = view.findViewById(R.id.btnLogout)
        nameTextView = view.findViewById(R.id.nameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)

        // Set click listener for the logout button
        btnLogout.setOnClickListener {
            logoutUser()
        }

        // Load user profile information
        loadUserProfile()

        return view
    }

    private fun loadUserProfile() {
        val user: FirebaseUser? = auth.currentUser
        if (user != null) {
            // User is signed in, get their information
            val userName = user.displayName
            val userEmail = user.email

            // Set the TextViews
            // Use placeholder if displayName is null or empty
            nameTextView.text = if (!userName.isNullOrEmpty()) userName else "username"
            emailTextView.text = userEmail ?: "user.email@example.com" // Use placeholder if email is null

            // Glide.with(this).load(user.photoUrl).into(profileImageView)
        } else {
            nameTextView.text = "User Name"
            emailTextView.text = "user.email@example.com"
        }
    }

    private fun logoutUser() {
        // Sign out from Firebase
        auth.signOut()

        // Navigate to StartActivity
        val intent = Intent(requireContext(), StartActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}