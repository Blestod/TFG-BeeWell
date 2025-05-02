package com.example.tfg_beewell_app.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.response.ChangesResponse
import kotlinx.coroutines.delay

object VitalsChangesListener {

    private const val PREF_TOKEN = "hr_token"

    suspend fun listen(
        context: Context,
        onUpsert: suspend () -> Unit
    ) {
        /* ① cliente */
        val hc  = HealthConnectClient.getOrCreate(context)

        /* ② token persistente */
        val prefs = context.getSharedPreferences("hc_prefs", Context.MODE_PRIVATE)
        var token = prefs.getString(PREF_TOKEN, null)
            ?: hc.getChangesToken(                       // ← necesita ChangesTokenRequest
                ChangesTokenRequest(
                    setOf(HeartRateRecord::class)    // qué tipos vigilar
                )
            ).also { prefs.edit().putString(PREF_TOKEN, it).apply() }

        /* ③ bucle */
        while (true) {

            val delta: ChangesResponse = hc.getChanges(token)

            for (change in delta.changes) {
                if (change is UpsertionChange) {         // ← UpsertionChange en 1.1.x
                    onUpsert()
                }
                /* DeletionChange → se ignora */
            }

            if (delta.changes.isEmpty()) delay(5_000)

            token = delta.nextChangesToken
            prefs.edit().putString(PREF_TOKEN, token).apply()
        }
    }
}
