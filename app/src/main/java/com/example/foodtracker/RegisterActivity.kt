package com.example.foodtracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.ui.semantics.text

class RegisterActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        emailInput = findViewById(R.id.register_email_input)
        passwordInput = findViewById(R.id.register_password_input)
        confirmPasswordInput = findViewById(R.id.register_confirm_password_input)
        registerBtn = findViewById(R.id.register_register_btn)

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            // Basic registration check
            if (password == confirmPassword) {
                // Registration successful
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                // Navigate to the login screen or the main app screen
                finish() // Go back to the previous activity
            } else {
                // Registration failed
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}