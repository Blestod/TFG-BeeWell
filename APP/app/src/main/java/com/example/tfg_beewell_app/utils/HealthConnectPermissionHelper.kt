package com.example.tfg_beewell_app.utils

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import kotlinx.coroutines.*

class HealthConnectPermissionHelper(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    /* ── permisos Health Connect que leeremos ── */
    private val hcPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    )

    /* ── permiso de sistema para lectura en 2.º plano (solo API 34+) ── */
    private val bgPermission = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

    /* --------------- utils --------------- */

    /**  Versión asíncrona con callback */
    fun hasAllHcPermissions(cb: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val granted = client.permissionController.getGrantedPermissions()
            cb(granted.containsAll(hcPermissions))
        }
    }

    /**  Versión sincrónica usada por HomeFragment  */
    fun hasAllHcPermissionsSync(): Boolean = runBlocking {
        client.permissionController.getGrantedPermissions().containsAll(hcPermissions)
    }

    /* --------------- getters usados fuera --------------- */

    fun getHcPermissions(): Set<String> = hcPermissions
    fun getBgPermission(): String = bgPermission
    fun bgPermissionNeeded(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}
