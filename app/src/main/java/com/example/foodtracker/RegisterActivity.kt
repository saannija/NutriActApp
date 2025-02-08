package com.example.foodtracker

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerBtn: Button
    private lateinit var imageView: ImageView
    private lateinit var loginBtn: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = Firebase.auth
        // Initialize Firestore
        db = Firebase.firestore

        emailInput = findViewById(R.id.register_email_input)
        passwordInput = findViewById(R.id.register_password_input)
        confirmPasswordInput = findViewById(R.id.register_confirm_password_input)
        registerBtn = findViewById(R.id.register_register_btn)
        imageView = findViewById(R.id.logoIcon)
        loginBtn = findViewById(R.id.already_member_btn)

        val whiteColor = ContextCompat.getColor(this, R.color.white)
        imageView.setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN)

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (password == confirmPassword) {
                // Firebase Authentication registration
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Registration success
                            Toast.makeText(this, "Registration successful.", Toast.LENGTH_SHORT).show()
                            // Get the current user's UID
                            val userId = auth.currentUser!!.uid
                            // Add user data to Firestore
                            val user = hashMapOf(
                                "userId" to userId,
                                "email" to email,
                                "createdAt" to Timestamp.now()
                            )
                            db.collection("users")
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "User data added to Firestore.", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error adding user data to Firestore.", Toast.LENGTH_SHORT).show()
                                }
                            startActivity(Intent(this, HomeActivity::class.java))
                        } else {
                            // If registration fails
                            Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                // Passwords do not match
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            }
        }

        loginBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}