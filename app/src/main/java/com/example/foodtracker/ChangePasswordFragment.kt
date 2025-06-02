package com.example.foodtracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth


class ChangePasswordFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    private lateinit var currentPasswordEditText: TextInputEditText
    private lateinit var newPasswordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText

    private lateinit var currentPasswordInputLayout: TextInputLayout
    private lateinit var newPasswordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout

    private lateinit var btnConfirmPasswordChange: MaterialButton
    private lateinit var btnCancelPasswordChange: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)

        auth = FirebaseAuth.getInstance()

        initializeViews(view)
        setupClickListeners()

        return view
    }

    private fun initializeViews(view: View) {
        currentPasswordEditText = view.findViewById(R.id.currentPasswordEditText_change)
        newPasswordEditText = view.findViewById(R.id.newPasswordEditText_change)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText_change)

        currentPasswordInputLayout = view.findViewById(R.id.currentPasswordInputLayout_change)
        newPasswordInputLayout = view.findViewById(R.id.newPasswordInputLayout_change)
        confirmPasswordInputLayout = view.findViewById(R.id.confirmPasswordInputLayout_change)

        btnConfirmPasswordChange = view.findViewById(R.id.btnConfirmPasswordChange)
        btnCancelPasswordChange = view.findViewById(R.id.btnCancelPasswordChange)
    }

    private fun setupClickListeners() {
        btnCancelPasswordChange.setOnClickListener {
            parentFragmentManager.popBackStack() // Go back to the previous fragment
        }

        btnConfirmPasswordChange.setOnClickListener {
            if (validatePasswordInput()) {
                performPasswordChange()
            }
        }
    }

    private fun validatePasswordInput(): Boolean {
        clearErrors()
        var isValid = true
        val currentPassword = currentPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (currentPassword.isEmpty()) {
            currentPasswordInputLayout.error = "Current password is required"
            isValid = false
        }

        if (newPassword.isEmpty()) {
            newPasswordInputLayout.error = "New password is required"
            isValid = false
        } else if (newPassword.length < 6) {
            newPasswordInputLayout.error = "New password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.error = "Confirm new password is required"
            isValid = false
        } else if (newPassword.isNotEmpty() && newPassword != confirmPassword) {
            confirmPasswordInputLayout.error = "Passwords do not match"
            isValid = false
        }

        if (newPassword == currentPassword && newPassword.isNotEmpty()) {
            newPasswordInputLayout.error = "New password cannot be the same as the current password"
            isValid = false
        }

        return isValid
    }

    private fun clearErrors() {
        currentPasswordInputLayout.error = null
        newPasswordInputLayout.error = null
        confirmPasswordInputLayout.error = null
    }

    private fun performPasswordChange() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentPassword = currentPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()

        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                        Log.d("ChangePassword", "Password updated successfully.")
                        currentPasswordEditText.text = null
                        newPasswordEditText.text = null
                        confirmPasswordEditText.text = null
                        parentFragmentManager.popBackStack() // Go back
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChangePassword", "Error updating password", e)
                        newPasswordInputLayout.error = "Failed to update password: ${e.message}"
                        Toast.makeText(context, "Failed to update password: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChangePassword", "Error re-authenticating user", e)
                currentPasswordInputLayout.error = "Current password is incorrect or authentication failed."
                Toast.makeText(context, "Authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}