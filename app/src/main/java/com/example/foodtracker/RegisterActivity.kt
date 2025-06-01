package com.example.foodtracker

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var surnameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerBtn: Button
    private lateinit var imageView: ImageView
    private lateinit var loginBtn: TextView
    private lateinit var googleSignInButton: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var googleSignInHelper: GoogleSignInHelper

    private val TAG = "RegisterActivity"

    private val PASSWORD_PATTERN: Pattern = Pattern.compile(
        "^" +
                "(?=.*[0-9])" +
                "(?=.*[a-z])" +
                "(?=.*[A-Z])" +
                "(?=.*[@#$%^&+=!?])" +
                "(?=\\S+$)" +
                ".{8,}" +
                "$"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth
        db = Firebase.firestore

        initializeViews()
        setupGoogleSignInHelper()
        setupClickListeners()
    }

    private fun initializeViews() {
        usernameInput = findViewById(R.id.register_username_input)
        emailInput = findViewById(R.id.register_email_input)
        nameInput = findViewById(R.id.register_name_input)
        surnameInput = findViewById(R.id.register_surname_input)
        phoneInput = findViewById(R.id.register_phone_input)
        passwordInput = findViewById(R.id.register_password_input)
        confirmPasswordInput = findViewById(R.id.register_confirm_password_input)
        registerBtn = findViewById(R.id.register_register_btn)
        imageView = findViewById(R.id.logoIcon)
        loginBtn = findViewById(R.id.already_member_btn)
        googleSignInButton = findViewById(R.id.google_btn)

        val whiteColor = ContextCompat.getColor(this, R.color.white)
        imageView.setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN)
    }

    private fun setupGoogleSignInHelper() {
        googleSignInHelper = GoogleSignInHelper(
            activityContext = this,
            coroutineScope = lifecycleScope,
            authFlowType = AuthFlowType.SIGN_UP, // Specify this is for sign-up
            onAuthSuccessNavigation = {
                Log.d(TAG, "Google Sign-Up successful, navigating to home")
                enableRegistrationButtons()
                navigateToHome()
            },
            onAuthFailure = { exception, isCancellation ->
                Log.e(TAG, "Google Sign-Up failed: ${exception.message}", exception)
                enableRegistrationButtons()

                if (isCancellation) {
                    Toast.makeText(this, "Sign-up cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMessage = when {
                        exception.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your connection and try again."
                        exception.message?.contains("configuration", ignoreCase = true) == true ->
                            "Google Sign-Up configuration error. Please contact support."
                        else -> "Google Sign-Up failed: ${exception.localizedMessage ?: "Unknown error"}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            },
            auth = auth,
            db = db
        )
    }

    private fun setupClickListeners() {
        registerBtn.setOnClickListener {
            performEmailPasswordRegistration()
        }

        loginBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        googleSignInButton.setOnClickListener {
            Log.d(TAG, "Google Sign-In button clicked")
            disableRegistrationButtons()
            googleSignInHelper.startSignIn()
        }
    }

    private fun performEmailPasswordRegistration() {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val name = nameInput.text.toString().trim()
        val surname = surnameInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        // Validation
        if (!validateInputs(username, email, password, confirmPassword)) {
            return
        }

        disableRegistrationButtons()
        Toast.makeText(this, "Checking details...", Toast.LENGTH_SHORT).show()

        // Convert username to lowercase for storage and checking
        val lowercaseUsername = username.lowercase()

        // Check if username is already taken (case-insensitive)
        db.collection("users").whereEqualTo("username", lowercaseUsername).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result != null && !task.result!!.isEmpty) {
                        usernameInput.error = "Username already taken"
                        usernameInput.requestFocus()
                        enableRegistrationButtons()
                        Toast.makeText(this, "Username already taken. Please choose another.", Toast.LENGTH_LONG).show()
                    } else {
                        proceedWithFirebaseEmailPasswordRegistration(lowercaseUsername, email, name, surname, phone, password)
                    }
                } else {
                    enableRegistrationButtons()
                    Log.e(TAG, "Error checking username availability", task.exception)
                    Toast.makeText(this, "Error checking username: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validateInputs(username: String, email: String, password: String, confirmPassword: String): Boolean {
        if (username.isEmpty()) {
            usernameInput.error = "Username is required"
            usernameInput.requestFocus()
            return false
        }

        if (username.length < 3) {
            usernameInput.error = "Username must be at least 3 characters"
            usernameInput.requestFocus()
            return false
        }

        // Check for invalid characters in username
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            usernameInput.error = "Username can only contain letters, numbers, and underscores"
            usernameInput.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email address"
            emailInput.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            passwordInput.requestFocus()
            return false
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            passwordInput.error = "Password must be at least 8 characters, include an uppercase letter, a lowercase letter, a digit, and a special character (@#\$%^&+=!)."
            passwordInput.requestFocus()
            Toast.makeText(this, "Password is too weak.", Toast.LENGTH_LONG).show()
            return false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Confirm password is required"
            confirmPasswordInput.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            confirmPasswordInput.requestFocus()
            passwordInput.text.clear()
            confirmPasswordInput.text.clear()
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun proceedWithFirebaseEmailPasswordRegistration(
        username: String,
        email: String,
        name: String,
        surname: String,
        phone: String,
        password: String
    ) {
        Toast.makeText(this, "Registering...", Toast.LENGTH_SHORT).show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        Log.d(TAG, "Firebase Auth account created successfully for: $email")

                        val originalUsername = usernameInput.text.toString().trim()
                        val profileUpdates = userProfileChangeRequest {
                            displayName = originalUsername
                        }
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileUpdateTask ->
                                if (!profileUpdateTask.isSuccessful) {
                                    Log.w(TAG, "Failed to update profile display name for email/pass user.", profileUpdateTask.exception)
                                }
                            }

                        // Create user document in Firestore
                        createUserDocumentInFirestore(firebaseUser.uid, username, originalUsername, email, name, surname, phone)
                    } else {
                        enableRegistrationButtons()
                        Log.w(TAG, "Email/pass registration successful, but currentUser is null.")
                        Toast.makeText(this, "Registration failed: User not found after creation.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    enableRegistrationButtons()
                    Log.w(TAG, "Email/pass registration failed.", task.exception)

                    when (task.exception) {
                        is FirebaseAuthUserCollisionException -> {
                            emailInput.error = "Email address is already in use."
                            emailInput.requestFocus()
                            Toast.makeText(this, "This email address is already registered.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun createUserDocumentInFirestore(
        userId: String,
        username: String,
        displayUsername: String,
        email: String,
        name: String,
        surname: String,
        phone: String
    ) {
        val userDocument = hashMapOf<String, Any>(
            "userId" to userId,
            "username" to username,
            "displayUsername" to displayUsername,
            "email" to email,
            "createdAt" to Timestamp.now(),
            "signUpMethod" to "email_password"
        )

        // Add optional fields if they're not empty
        if (name.isNotEmpty()) userDocument["name"] = name
        if (surname.isNotEmpty()) userDocument["surname"] = surname
        if (phone.isNotEmpty()) userDocument["phone"] = phone

        db.collection("users").document(userId).set(userDocument)
            .addOnSuccessListener {
                Log.d(TAG, "Email/Pass registration successful & Firestore updated for $userId")
                navigateToHome()
            }
            .addOnFailureListener { e ->
                enableRegistrationButtons()
                Log.w(TAG, "Firestore save failed for email/pass user $userId", e)
                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_LONG).show()

                // Delete the Firebase Auth user since Firestore save failed
                auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                    Log.d(TAG, "Firebase Auth user deletion attempt after Firestore save failure. Success: ${deleteTask.isSuccessful}")
                }
            }
    }

    private fun enableRegistrationButtons() {
        registerBtn.isEnabled = true
        googleSignInButton.isEnabled = true
    }

    private fun disableRegistrationButtons() {
        registerBtn.isEnabled = false
        googleSignInButton.isEnabled = false
    }

    private fun navigateToHome() {
        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already signed in, navigating to home")
            navigateToHome()
        }
    }
}