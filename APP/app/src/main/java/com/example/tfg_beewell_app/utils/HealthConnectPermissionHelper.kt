package com.example.tfg_beewell_app.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class HealthConnectPermissionHelper(private val context: Context) {

    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class)

    )

    fun hasAllPermissions(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            callback(granted.containsAll(permissions))
        }
    }


    fun getRequiredPermissions(): Set<String> = permissions



    fun allGranted(granted: Set<String>): Boolean =
        granted.containsAll(getRequiredPermissions())
}


