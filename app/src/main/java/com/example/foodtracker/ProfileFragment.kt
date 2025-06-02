package com.example.foodtracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment() {

    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnSettings: MaterialButton
    private lateinit var btnNotifications: MaterialButton
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var profileImageView: ShapeableImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var userDataListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews(view)
        setupClickListeners()
        loadUserProfile()

        return view
    }

    private fun initializeViews(view: View) {
        btnLogout = view.findViewById(R.id.btnLogout)
        btnEditProfile = view.findViewById(R.id.editProfileButton)
        btnSettings = view.findViewById(R.id.settingsButton)
        btnNotifications = view.findViewById(R.id.notificationsButton)
        nameTextView = view.findViewById(R.id.nameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        profileImageView = view.findViewById(R.id.profileImageView)
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            logoutUser()
        }

        btnEditProfile.setOnClickListener {
            // Navigate to EditProfileFragment using FragmentTransaction
            val editProfileFragment = EditProfileFragment()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, editProfileFragment)
                .addToBackStack(null)
                .commit()
        }

        btnSettings.setOnClickListener {
            // Navigate to settings or show settings options
            Toast.makeText(context, "Settings feature coming soon", Toast.LENGTH_SHORT).show()
        }

        btnNotifications.setOnClickListener {
            // Navigate to notifications settings
            Toast.makeText(context, "Notifications settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        val user: FirebaseUser? = auth.currentUser
        if (user != null) {
            // Listen for real-time updates to user data
            userDataListener = firestore.collection("users").document(user.uid)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Toast.makeText(context, "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        updateUIWithUserData(user, document.data)
                    } else {
                        // Document doesn't exist, create it with basic info
                        createUserDocument(user)
                    }
                }
        } else {
            // User not signed in, show placeholder data
            nameTextView.text = "Guest User"
            emailTextView.text = "Not signed in"
        }
    }

    private fun updateUIWithUserData(user: FirebaseUser, userData: Map<String, Any>?) {
        val name = userData?.get("name") as? String ?: ""
        val surname = userData?.get("surname") as? String ?: ""
        val username = userData?.get("username") as? String ?: ""
        val email = user.email ?: ""

        // Determine what to display as the name
        val displayName = when {
            name.isNotEmpty() && surname.isNotEmpty() -> "$name $surname"
            name.isNotEmpty() -> name
            user.displayName?.isNotEmpty() == true -> user.displayName
            username.isNotEmpty() -> username
            else -> "User"
        }

        nameTextView.text = displayName
        emailTextView.text = email

        // Glide.with(this).load(user.photoUrl).placeholder(R.drawable.icon_user).into(profileImageView)
    }

    private fun createUserDocument(user: FirebaseUser) {
        val userMap = hashMapOf(
            "email" to user.email,
            "username" to (user.email?.substringBefore("@")?.lowercase() ?: "user"),
            "name" to (user.displayName?.split(" ")?.firstOrNull() ?: ""),
            "surname" to (user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""),
            "phone" to null,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("users").document(user.uid)
            .set(userMap)
            .addOnSuccessListener {
                // Document created successfully
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error creating user profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutUser() {
        userDataListener?.remove()

        // Sign out from Firebase
        auth.signOut()

        // Navigate to StartActivity
        val intent = Intent(requireContext(), StartActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userDataListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        // Reload user profile when returning to this fragment
        loadUserProfile()
    }
}