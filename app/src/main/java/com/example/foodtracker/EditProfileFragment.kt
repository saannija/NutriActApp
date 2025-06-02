package com.example.foodtracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Tasks
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var nameEditText: TextInputEditText
    private lateinit var surnameEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText

    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var surnameInputLayout: TextInputLayout
    private lateinit var usernameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var phoneInputLayout: TextInputLayout

    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnGoToChangePassword: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews(view)
        loadCurrentUserData()
        setupClickListeners()

        return view
    }

    private fun initializeViews(view: View) {
        nameEditText = view.findViewById(R.id.nameEditText)
        surnameEditText = view.findViewById(R.id.surnameEditText)
        usernameEditText = view.findViewById(R.id.usernameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)

        nameInputLayout = view.findViewById(R.id.nameInputLayout)
        surnameInputLayout = view.findViewById(R.id.surnameInputLayout)
        usernameInputLayout = view.findViewById(R.id.usernameInputLayout)
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        phoneInputLayout = view.findViewById(R.id.phoneInputLayout)

        btnSaveChanges = view.findViewById(R.id.btnSaveChanges)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnGoToChangePassword = view.findViewById(R.id.btnGoToChangePassword)
    }

    private fun loadCurrentUserData() {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nameEditText.setText(document.getString("name") ?: "")
                        surnameEditText.setText(document.getString("surname") ?: "")
                        usernameEditText.setText(document.getString("username") ?: user.displayName ?: "")
                        phoneEditText.setText(document.getString("phone") ?: "")
                    }
                    emailEditText.setText(user.email ?: "")
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSaveChanges.setOnClickListener {
            if (validateProfileInput()) {
                saveProfileChanges()
            }
        }

        btnGoToChangePassword.setOnClickListener {
            // Manually navigate to ChangePasswordFragment
            val changePasswordFragment = ChangePasswordFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, changePasswordFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun validateProfileInput(): Boolean {
        clearErrors()
        var isValid = true

        if (usernameEditText.text.toString().trim().isEmpty()) {
            usernameInputLayout.error = "Username is required"
            isValid = false
        }

        val emailValue = emailEditText.text.toString().trim()
        if (emailValue.isEmpty()) {
            emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            emailInputLayout.error = "Please enter a valid email"
            isValid = false
        }
        return isValid
    }

    private fun clearErrors() {
        nameInputLayout.error = null
        surnameInputLayout.error = null
        usernameInputLayout.error = null
        emailInputLayout.error = null
        phoneInputLayout.error = null
    }

    private fun saveProfileChanges() {
        val user = auth.currentUser ?: return
        val newEmail = emailEditText.text.toString().trim()
        val newUsername = usernameEditText.text.toString().trim().lowercase()
        val emailIsBeingChanged = newEmail != user.email

        if (emailIsBeingChanged) {
            Log.w("EditProfile", "Email change initiated. Handle potential 'ERROR_REQUIRES_RECENT_LOGIN'.")
        }
        checkUniquenessAndSave(user, newEmail, newUsername, emailIsBeingChanged)
    }

    private fun checkUniquenessAndSave(user: FirebaseUser, newEmail: String, newUsername: String, emailIsBeingChanged: Boolean) {
        firestore.collection("users")
            .whereEqualTo("username", newUsername)
            .get()
            .addOnSuccessListener { usernameQuery ->
                val isUsernameUnique = usernameQuery.isEmpty ||
                        (usernameQuery.size() == 1 && usernameQuery.documents[0].id == user.uid)

                if (!isUsernameUnique) {
                    usernameInputLayout.error = "Username is already taken"
                    return@addOnSuccessListener
                }

                if (emailIsBeingChanged) {
                    firestore.collection("users")
                        .whereEqualTo("email", newEmail)
                        .get()
                        .addOnSuccessListener { emailQuery ->
                            val isEmailUnique = emailQuery.isEmpty ||
                                    (emailQuery.size() == 1 && emailQuery.documents[0].id == user.uid)

                            if (!isEmailUnique) {
                                emailInputLayout.error = "Email is already registered"
                                return@addOnSuccessListener
                            }
                            updateUserProfileData(user, newEmail, newUsername, true)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error checking email: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    updateUserProfileData(user, newEmail, newUsername, false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error checking username: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfileData(user: FirebaseUser, newEmail: String, newUsername: String, emailWasChanged: Boolean) {
        val name = nameEditText.text.toString().trim()
        val surname = surnameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        val authTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

        if (emailWasChanged && newEmail != user.email) {
            authTasks.add(user.updateEmail(newEmail).addOnFailureListener { exception ->
                Log.e("EditProfile", "Failed to update email in Auth", exception)
                Toast.makeText(context, "Failed to update email: ${exception.message}", Toast.LENGTH_LONG).show()
                if (exception.message?.contains("requires recent login", ignoreCase = true) == true) {
                    emailInputLayout.error = "Updating email requires recent login. Please re-authenticate."
                }
            })
        }

        val currentDisplayName = user.displayName
        val newDisplayName = if (name.isNotEmpty() && surname.isNotEmpty()) {
            "$name $surname"
        } else if (name.isNotEmpty()) {
            name
        } else {
            newUsername
        }

        if (newDisplayName != currentDisplayName && newDisplayName.isNotBlank()) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName)
                .build()
            authTasks.add(user.updateProfile(profileUpdates).addOnFailureListener { exception ->
                Log.e("EditProfile", "Failed to update display name in Auth", exception)
            })
        }

        Tasks.whenAllSuccess<Void>(authTasks).addOnSuccessListener {
            val userMap = hashMapOf(
                "name" to name.ifEmpty { null },
                "surname" to surname.ifEmpty { null },
                "username" to newUsername,
                "email" to newEmail,
                "phone" to phone.ifEmpty { null },
                "updatedAt" to Timestamp.now()
            )

            firestore.collection("users").document(user.uid)
                .update(userMap as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Log.e("EditProfile", "Failed to update profile in Firestore", e)
                    Toast.makeText(context, "Failed to update profile details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { exception ->
            Log.e("EditProfile", "One or more Auth update tasks failed.", exception)
        }
    }
}