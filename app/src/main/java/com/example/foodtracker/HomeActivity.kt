package com.example.foodtracker

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.foodtracker.util.AppNotificationManager
import com.example.foodtracker.workers.ExpiryCheckWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "NutriAct"

        // Initialize WorkManager for expiry notifications
        setupExpiryNotifications()

        // Handle notification tap from system notification
        handleNotificationIntent()

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

        // Load the default fragment (HomeFragment)
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment is AddFragment) {
                    val addFragment = currentFragment as AddFragment
                    if (addFragment.isEditing()) {
                        supportFragmentManager.popBackStack()
                    } else {
                        addFragment.showAddOptions()
                    }
                } else if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupExpiryNotifications() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val expiryCheckRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
            4, TimeUnit.HOURS,
            15, TimeUnit.MINUTES  // Flex interval
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "expiry_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            expiryCheckRequest
        )

        Log.d("HomeActivity", "Expiry notification work scheduled")
    }

    private fun handleNotificationIntent() {
        if (intent.getBooleanExtra("open_notifications", false)) {
            // Navigate to notifications fragment
            loadFragment(NotificationsFragment())
        }
    }

    // Helper function to load fragments
    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        // Only add to back stack if it's not the initial fragment
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            transaction.addToBackStack(null)
        }
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
            R.id.action_profile -> {
                loadFragment(ProfileFragment())
                true
            }
            R.id.action_test_notifications -> {
                testNotifications()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Test methods for debugging notifications
    private fun testNotifications() {
        // Show dialog to choose test type
        val options = arrayOf(
            "Test System Notification",
            "Test Expiry Check Worker",
            "Cancel All Work"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Test Notifications")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createTestSystemNotification()
                    1 -> testExpiryCheckWorker()
                    2 -> cancelAllWork()
                }
            }
            .show()
    }

    private fun createTestSystemNotification() {
        val notificationManager = AppNotificationManager(this)
        notificationManager.showExpiryNotification(
            "Test Product",
            2,
            "test_id_123"
        )
        Toast.makeText(this, "Test system notification sent", Toast.LENGTH_SHORT).show()
    }

    private fun testExpiryCheckWorker() {
        // This will trigger the expiry check worker immediately
        val testWorkRequest = OneTimeWorkRequestBuilder<ExpiryCheckWorker>()
            .build()

        WorkManager.getInstance(this).enqueue(testWorkRequest)

        // Monitor the work
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(testWorkRequest.id)
            .observe(this) { workInfo ->
                if (workInfo != null) {
                    Log.d("TestNotification", "Work Status: ${workInfo.state}")
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            Toast.makeText(this, "Expiry check completed successfully", Toast.LENGTH_SHORT).show()
                        }
                        WorkInfo.State.FAILED -> {
                            Toast.makeText(this, "Expiry check failed", Toast.LENGTH_SHORT).show()
                        }
                        WorkInfo.State.RUNNING -> {
                            Toast.makeText(this, "Checking for expiring products...", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Other states like ENQUEUED, BLOCKED, CANCELLED
                        }
                    }
                }
            }
    }

    private fun cancelAllWork() {
        WorkManager.getInstance(this).cancelUniqueWork("expiry_check_work")
        Toast.makeText(this, "All notification work cancelled", Toast.LENGTH_SHORT).show()

        // Reschedule after a short delay if needed
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reschedule Work?")
            .setMessage("Do you want to reschedule the expiry check work?")
            .setPositiveButton("Yes") { _, _ ->
                setupExpiryNotifications()
                Toast.makeText(this, "Work rescheduled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}