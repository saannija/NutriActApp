package com.example.foodtracker

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
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
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment())
                    true
                }
                R.id.nav_fridge -> {
                    loadFragment(InventoryFragment())
                    true
                }
                R.id.nav_add -> {
                    loadFragment(AddFragment())
                    true
                }
                else -> false
            }
        }

        // Load the HomeFragment initially
        loadFragment(HomeFragment())

        // Set up the OnBackPressedCallback
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment is AddFragment) {
                    if (currentFragment.isEditing()) {
                        // If editing, navigate back to InventoryFragment
                        supportFragmentManager.popBackStack()
                    } else {
                        // If adding new, show the add options
                        currentFragment.showAddOptions()
                    }
                } else {
                    // For other fragments, use default back behavior (exit the app)
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    // Helper function to load fragments
    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        // transaction.addToBackStack(null) // Removed: Do not add to back stack for bottom nav
        transaction.commit()
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
                // Handle notifications click
                true
            }
            R.id.action_profile -> {
                loadFragment(ProfileFragment())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}