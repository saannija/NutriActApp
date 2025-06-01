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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var emailUsernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: TextView
    private lateinit var googleSignInButton: ImageView
    private lateinit var imageView: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var googleSignInHelper: GoogleSignInHelper

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth and Firestore
        auth = Firebase.auth
        db = Firebase.firestore

        // Check if user is already logged in
        if (auth.currentUser != null) {
            Log.d(TAG, "User already logged in, navigating to home")
            navigateToHome()
            return
        }

        initializeViews()
        setupGoogleSignInHelper()
        setupClickListeners()
    }

    private fun initializeViews() {
        emailUsernameInput = findViewById(R.id.email_input) // Accept email or username
        passwordInput = findViewById(R.id.password_input)
        loginBtn = findViewById(R.id.login_btn)
        registerBtn = findViewById(R.id.register_btn)
        googleSignInButton = findViewById(R.id.google_btn)
        imageView = findViewById(R.id.logoIcon)

        val whiteColor = ContextCompat.getColor(this, R.color.white)
        imageView.setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN)
    }

    private fun setupGoogleSignInHelper() {
        googleSignInHelper = GoogleSignInHelper(
            activityContext = this,
            coroutineScope = lifecycleScope,
            authFlowType = AuthFlowType.SIGN_IN,
            onAuthSuccessNavigation = {
                Log.d(TAG, "Google Sign-In successful, navigating to home")
                enableLoginButtons()
                navigateToHome()
            },
            onAuthFailure = { exception, isCancellation ->
                Log.e(TAG, "Google Sign-In failed: ${exception.message}", exception)
                enableLoginButtons()

                if (isCancellation) {
                    Toast.makeText(this, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMessage = when {
                        exception.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your connection and try again."
                        exception.message?.contains("configuration", ignoreCase = true) == true ->
                            "Google Sign-In configuration error. Please contact support."
                        else -> "Google Sign-In failed: ${exception.localizedMessage ?: "Unknown error"}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            },
            auth = auth,
            db = db
        )
    }

    private fun setupClickListeners() {
        loginBtn.setOnClickListener {
            performLogin()
        }

        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        googleSignInButton.setOnClickListener {
            Log.d(TAG, "Google Sign-In button clicked")
            disableLoginButtons()
            googleSignInHelper.startSignIn()
        }
    }

    private fun performLogin() {
        val emailOrUsername = emailUsernameInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (emailOrUsername.isEmpty()) {
            emailUsernameInput.error = "Email or username is required"
            emailUsernameInput.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            passwordInput.requestFocus()
            return
        }

        disableLoginButtons()
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show()

        // Check if input is an email or username
        if (Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
            // It's an email, sign in directly
            signInWithEmail(emailOrUsername, password)
        } else {
            // It's a username, need to find the associated email first
            findEmailByUsername(emailOrUsername.lowercase(), password)
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                enableLoginButtons()
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sign-in successful")
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                } else {
                    Log.w(TAG, "Email sign-in failed", task.exception)
                    val errorMessage = when {
                        task.exception?.message?.contains("There is no user record") == true ->
                            "Invalid email/username or password."
                        task.exception?.message?.contains("password is invalid") == true ->
                            "Invalid email/username or password."
                        task.exception?.message?.contains("email address is badly formatted") == true ->
                            "Please enter a valid email address."
                        task.exception?.message?.contains("too many unsuccessful login attempts") == true ->
                            "Too many failed attempts. Please try again later."
                        else -> "Login failed. Please check your credentials and try again."
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun findEmailByUsername(username: String, password: String) {
        // Query Firestore to find user with matching username (case-insensitive)
        db.collection("users")
            .whereEqualTo("username", username.lowercase())
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val foundEmail = document.getString("email")

                    if (foundEmail != null) {
                        Log.d(TAG, "Found email for username: $username")
                        signInWithEmail(foundEmail, password)
                    } else {
                        enableLoginButtons()
                        Log.e(TAG, "User document found but email is null")
                        Toast.makeText(this, "Account error. Please contact support.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    enableLoginButtons()
                    Log.d(TAG, "No user found with username: $username")
                    Toast.makeText(this, "Invalid email/username or password.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                enableLoginButtons()
                Log.e(TAG, "Error finding user by username", exception)
                Toast.makeText(this, "Connection error. Please try again.", Toast.LENGTH_LONG).show()
            }
    }

    private fun enableLoginButtons() {
        loginBtn.isEnabled = true
        googleSignInButton.isEnabled = true
    }

    private fun disableLoginButtons() {
        loginBtn.isEnabled = false
        googleSignInButton.isEnabled = false
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }
}