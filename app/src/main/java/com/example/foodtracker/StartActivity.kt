package com.example.foodtracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {

    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        loginBtn = findViewById(R.id.start_login_btn)
        registerBtn = findViewById(R.id.start_register_btn)

        loginBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}