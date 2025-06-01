package com.example.foodtracker

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class StartActivity : AppCompatActivity() {

    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var imageView: ImageView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        auth = FirebaseAuth.getInstance()
        loginBtn = findViewById(R.id.start_login_btn)
        registerBtn = findViewById(R.id.start_register_btn)
        imageView = findViewById(R.id.logoIcon)

        val whiteColor = ContextCompat.getColor(this, R.color.white)
        imageView.setColorFilter(whiteColor, PorterDuff.Mode.SRC_IN)

        // Check if the user is already logged in
        if (auth.currentUser != null) {
            // User is logged in, go to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            // User is not logged in, show login/register options
            loginBtn.setOnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
            }

            registerBtn.setOnClickListener {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
        }
    }
}