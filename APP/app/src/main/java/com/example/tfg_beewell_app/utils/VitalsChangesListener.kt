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
        val hc = HealthConnectClient.getOrCreate(context)

        val session = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val email = session.getString("user_email", null) ?: return

        val prefs = context.getSharedPreferences("hc_prefs_$email", Context.MODE_PRIVATE)
        var token = prefs.getString(PREF_TOKEN, null)
            ?: hc.getChangesToken(
                ChangesTokenRequest(setOf(HeartRateRecord::class))
            ).also {
                prefs.edit().putString(PREF_TOKEN, it).apply()
            }

        while (true) {
            val delta: ChangesResponse = hc.getChanges(token)

            for (change in delta.changes) {
                if (change is UpsertionChange) {
                    onUpsert()
                }
            }

            if (delta.changes.isEmpty()) delay(5_000)

            token = delta.nextChangesToken
            prefs.edit().putString(PREF_TOKEN, token).apply()
        }
    }
}
