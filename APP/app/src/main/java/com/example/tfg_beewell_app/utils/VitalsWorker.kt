package com.example.tfg_beewell_app.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.tfg_beewell_app.Constants
import com.example.tfg_beewell_app.ui.VitalData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

class VitalsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            println("🔄 Ejecutando VitalsWorker")

            val prefs = applicationContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val email = prefs.getString("user_email", null)
            if (email == null) {
                Log.e("VitalsWorker", "❌ No se encontró el email del usuario.")
                return@withContext Result.failure()
            }

            // ✅ Use per-user scoped key
            val lastSavedTimeKey = "last_vital_time_$email"
            val lastSavedTime = prefs.getInt(lastSavedTimeKey, 0)

            val vital = HealthReader.getLatestVitals(applicationContext, email)
            if (vital == null) {
                Log.e("VitalsWorker", "❌ No se pudo obtener vitales desde HealthReader.")
                return@withContext Result.retry()
            }

            val vitalTime = vital.vitalTime
            if (vitalTime == null) {
                Log.e("VitalsWorker", "❌ vitalTime es null.")
                return@withContext Result.success()
            }

            if (vitalTime <= lastSavedTime) {
                Log.d("VitalsWorker", "🔁 No nuevos datos. Último enviado: $lastSavedTime")
                return@withContext Result.success()
            }

            if (isVitalDataEmpty(vital)) {
                Log.w("VitalsWorker", "🚫 No se enviaron datos: todos los campos están vacíos o nulos.")
                return@withContext Result.success()
            }

            val body = JSONObject().apply {
                put("user_email", vital.userEmail)
                put("vital_time", vital.vitalTime)
                put("glucose_value", vital.glucoseValue ?: JSONObject.NULL)
                put("heart_rate", vital.heartRate ?: JSONObject.NULL)
                put("temperature", vital.temperature ?: JSONObject.NULL)
                put("calories", vital.calories ?: JSONObject.NULL)
                put("sleep_duration", vital.sleepDuration ?: JSONObject.NULL)
                put("oxygen_saturation", vital.oxygenSaturation ?: JSONObject.NULL)
            }

            Log.d("VitalsWorker", "📤 Enviando datos: $body")

            val queue = Volley.newRequestQueue(applicationContext)

            return@withContext suspendCancellableCoroutine { continuation ->
                val request = object : StringRequest(
                    Method.POST,
                    Constants.BASE_URL + "/vital",
                    Response.Listener { response ->
                        Log.d("VitalsWorker", "✅ Vitals enviados correctamente: $response")
                        prefs.edit().putInt(lastSavedTimeKey, vitalTime).apply()
                        continuation.resume(Result.success(), null)
                    },
                    Response.ErrorListener { error ->
                        val statusCode = error.networkResponse?.statusCode
                        val data = error.networkResponse?.data?.toString(Charsets.UTF_8)
                        Log.e("VitalsWorker", "❌ Error al enviar vitals: HTTP $statusCode\nBody: $data", error)
                        continuation.resume(Result.retry(), null)
                    }
                ) {
                    override fun getBodyContentType(): String = "application/json; charset=utf-8"
                    override fun getBody(): ByteArray = body.toString().toByteArray(Charsets.UTF_8)
                }

                queue.add(request)
            }

        } catch (e: Exception) {
            Log.e("VitalsWorker", "‼️ Excepción inesperada en VitalsWorker: ${e.message}", e)
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    private fun isVitalDataEmpty(v: VitalData): Boolean {
        return (v.glucoseValue == null || v.glucoseValue == 0) &&
                (v.heartRate == null || v.heartRate == 0f) &&
                (v.temperature == null || v.temperature == 0f) &&
                (v.calories == null || v.calories == 0f) &&
                (v.sleepDuration == null || v.sleepDuration == 0) &&
                (v.oxygenSaturation == null || v.oxygenSaturation == 0f)
    }
}
