package com.example.tfg_beewell_app.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.response.ChangesResponse
import kotlinx.coroutines.delay
import kotlin.math.min

object VitalsChangesListener {

    private const val PREF_TOKEN = "hr_token"

    //-- Intervalos (ms) -----------------------------------------------------------
    private const val POLL_INTERVAL_MS = 5_000L        // espera normal entre llamadas
    private const val QUOTA_BACKOFF_MS = 30_000L       // espera inicial tras “quota exceeded”
    private const val MAX_BACKOFF_MS   = 5 * 60_000L   // tope del back-off exponencial
    //-----------------------------------------------------------------------------

    suspend fun listen(
        context: Context,
        onUpsert: suspend () -> Unit
    ) {
        val hc = HealthConnectClient.getOrCreate(context)

        // Sesión de usuario
        val session = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val email = session.getString("user_email", null) ?: return

        // Preferencias privadas por usuario
        val prefs = context.getSharedPreferences("hc_prefs_$email", Context.MODE_PRIVATE)

        // Obtener (o crear) el token de cambios
        var token = prefs.getString(PREF_TOKEN, null)
            ?: hc.getChangesToken(
                ChangesTokenRequest(setOf(HeartRateRecord::class))
            ).also { prefs.edit().putString(PREF_TOKEN, it).apply() }

        var backoff = POLL_INTERVAL_MS

        while (true) {
            try {
                val delta: ChangesResponse = hc.getChanges(token)

                // Procesar inserciones/actualizaciones
                delta.changes.forEach { change ->
                    if (change is UpsertionChange) onUpsert()
                }

                // Guardar nuevo token
                token = delta.nextChangesToken
                prefs.edit().putString(PREF_TOKEN, token).apply()

                // Reset back-off y espera fija
                backoff = POLL_INTERVAL_MS
                delay(POLL_INTERVAL_MS)

            } catch (e: Exception) {
                // “quota exceeded” → back-off exponencial, cualquier otro error → espera corta
                if (e.message?.contains("quota exceeded", ignoreCase = true) == true) {
                    delay(backoff)
                    backoff = min(backoff * 2, MAX_BACKOFF_MS)
                } else {
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }
}
