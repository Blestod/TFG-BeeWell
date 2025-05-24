package com.example.tfg_beewell_app.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tfg_beewell_app.Constants
import com.example.tfg_beewell_app.local.GlucoseDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class MonthlyInsightWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = GlucoseDB.getInstance(applicationContext).historyDao()

            val now = Instant.now()
            val start = now.minus(30, ChronoUnit.DAYS).epochSecond
            val end = now.epochSecond

            val rows = dao.range(start, end)

            if (rows.isEmpty()) {
                Log.d("InsightWorker", "No hay datos del último mes.")
                return@withContext Result.success()
            }

            val values = rows.joinToString(separator = "\n") {
                "timestamp=${it.timestamp}, value=${it.glucoseValue}"
            }

            val email = "test" // ⚠️ puedes hacerlo dinámico si tu app lo guarda en SharedPreferences
            val summary = fetchSummaryFromServer(email, values)

            val prefs = applicationContext.getSharedPreferences("monthly_summary", Context.MODE_PRIVATE)
            val currentMonth = LocalDate.now().withDayOfMonth(1).toString() // ej: "2025-05-01"
            prefs.edit().putString("summary_$currentMonth", summary).apply()

            Log.d("InsightWorker", "✅ Resumen mensual generado.")
            Result.success()
        } catch (e: Exception) {
            Log.e("InsightWorker", "❌ Error al generar el resumen mensual", e)
            Result.retry()
        }
    }

    private fun fetchSummaryFromServer(email: String, values: String): String {
        val url = "${Constants.BASE_URL}/generate_summary"

        val json = JSONObject().apply {
            put("user_email", email)
            put("values", values)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()


        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                Log.e("MonthlyInsight", "❌ Error ${response.code}:\n$responseBody")
                return "No se pudo generar resumen."
            }

            val json = JSONObject(responseBody ?: "")
            json.getString("summary")
        } catch (e: Exception) {
            Log.e("MonthlyInsight", "❌ Error al contactar backend", e)
            "Error de conexión."
        }
    }
}