package com.example.foodtracker

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "NutriAct"

        // Set up the bottom navigation view
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_search -> {
                    Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_fridge -> {
                    Toast.makeText(this, "Fridge", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_add -> {
                    Toast.makeText(this, "Add", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    // Inflate the menu for the toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    // Handle toolbar item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_profile -> {
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}