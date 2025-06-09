package com.example.tfg_beewell_app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tfg_beewell_app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class HealthDataService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())

        // Start periodic task, like using a coroutine or handler to poll Health Connect
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                // Your logic to read from Health Connect and send to server
                delay(15 * 60 * 1000) // every 15 min
            }
        }
        Log.d("HealthService", "Foreground service started");
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "health_data_channel"
        val channelName = "Health Monitoring"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d("HealthService", "Creating notification");

        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("BeeWell is running")
            .setContentText("Monitoring your health data...")
            .setSmallIcon(R.drawable.beehealthylogohiteoutline)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
