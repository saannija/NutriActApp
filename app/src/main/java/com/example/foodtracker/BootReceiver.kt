package com.example.foodtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodtracker.workers.ExpiryCheckWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scheduleExpiryCheck(context)
        }
    }

    private fun scheduleExpiryCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val expiryCheckRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
            4, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "expiry_check_work",
            ExistingPeriodicWorkPolicy.KEEP,
            expiryCheckRequest
        )
    }
}