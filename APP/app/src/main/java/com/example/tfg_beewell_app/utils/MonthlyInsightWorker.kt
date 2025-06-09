package com.example.tfg_beewell_app.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tfg_beewell_app.Constants
import com.example.tfg_beewell_app.local.GlucoseDB
import com.example.tfg_beewell_app.local.LocalGlucoseHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

class MonthlyInsightWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @Suppress("LongLogTag")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("MonthlyInsightWorker", "🚀 MonthlyInsightWorker doWork() started")
        try {
            val prefs = applicationContext
                .getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val email = prefs.getString("user_email", null)
            if (email.isNullOrEmpty()) {
                Log.e("MonthlyInsightWorker", "❌ No user email in session.")
                return@withContext Result.failure()
            }

            val dao = GlucoseDB.getInstance(applicationContext).historyDao()
            val allRows = dao.getAll()
            if (allRows.isEmpty()) {
                Log.d("MonthlyInsightWorker", "ℹ️ No glucose history entries.")
                return@withContext Result.success()
            }

            val zone = ZoneId.systemDefault()
            val summaryPrefs = applicationContext
                .getSharedPreferences("monthly_summary_$email", Context.MODE_PRIVATE)

            val entriesByMonth = allRows.groupBy { entry ->
                Instant.ofEpochSecond(entry.timestamp.toLong())
                    .atZone(zone)
                    .toLocalDate()
                    .withDayOfMonth(1)
                    .toString()
            }

            var generatedCount = 0
            for ((monthKey, entries) in entriesByMonth) {
                if (summaryPrefs.contains("summary_$monthKey")) {
                    Log.d("MonthlyInsightWorker", "📘 Summary already exists for $monthKey")
                    continue
                }

                val values = entries.joinToString(separator = "\n") { e ->
                    "timestamp=${e.timestamp}, value=${e.glucoseValue}"
                }

                val summary = fetchSummaryFromServer(email, values)

                if (summary == "A summary could not be made yet."
                    || summary == "Error de conexión.") {
                    Log.w("MonthlyInsightWorker",
                        "🔁 Invalid summary for $monthKey, retrying")
                    return@withContext Result.retry()
                }

                summaryPrefs.edit()
                    .putString("summary_$monthKey", summary)
                    .apply()
                Log.d("MonthlyInsightWorker", "✅ Generated summary for $monthKey")
                generatedCount++
            }

            if (generatedCount == 0) {
                Log.d("MonthlyInsightWorker", "ℹ️ No new summaries needed.")
            }

            Log.d("MonthlyInsightWorker", "🏁 MonthlyInsightWorker doWork() completed successfully")
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e("MonthlyInsightWorker", "❌ Unexpected error in doWork()", e)
            return@withContext Result.retry()
        }
    }

    private fun fetchSummaryFromServer(email: String, values: String): String {
        val url = "${Constants.BASE_URL}/generate_summary"
        val json = JSONObject().apply {
            put("user_email", email)
            put("values", values)
        }
        val requestBody =
            json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        val client = OkHttpClient()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e("MonthlyInsightWorker", "❌ HTTP ${response.code} - $body")
                return "A summary could not be made yet."
            }
            JSONObject(body).optString("summary", "A summary could not be made yet.")
        } catch (e: Exception) {
            Log.e("MonthlyInsightWorker", "❌ Network error while fetching summary", e)
            "Error de conexión."
        }
    }
}