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
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: TextView
    private lateinit var imageView: ImageView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = Firebase.auth

        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginBtn = findViewById(R.id.login_btn)
        registerBtn = findViewById(R.id.register_btn)
        imageView = findViewById(R.id.logoIcon)

        val whiteColor = ContextCompat.getColor(this, R.color.white)
        imageView.setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            // Firebase Authentication login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success
                        Toast.makeText(this, "Login successful.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                    } else {
                        // If sign in fails
                        Toast.makeText(this, "Login failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        registerBtn.setOnClickListener {
            // Navigate to the registration screen
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}