package com.example.foodtracker

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

//import androidx.compose.ui.semantics.text

class MainActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: TextView
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            Log.i("Test Credentials", "Email: $email and Password: $password")
            if (email == "test@example.com" && password == "password") {
                // Login successful
                Log.i("Login", "Login successful")
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // Login failed
                Log.i("Login", "Login failed")
                Toast.makeText(this, "Login failed!", Toast.LENGTH_SHORT).show()
            }
            // Further login logic ...
        }
        registerBtn.setOnClickListener {
            // Navigate to the registration screen
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}