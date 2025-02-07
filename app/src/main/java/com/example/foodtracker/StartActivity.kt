package com.example.foodtracker

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class StartActivity : AppCompatActivity() {

    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        loginBtn = findViewById(R.id.start_login_btn)
        registerBtn = findViewById(R.id.start_register_btn)
        imageView = findViewById(R.id.logoIcon)

        val whiteColor = ContextCompat.getColor(this, R.color.white)
        imageView.setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN)

        loginBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}