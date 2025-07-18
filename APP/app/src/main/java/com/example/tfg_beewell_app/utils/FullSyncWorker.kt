package com.example.tfg_beewell_app.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.tfg_beewell_app.Constants
import com.example.tfg_beewell_app.local.GlucoseDB
import com.example.tfg_beewell_app.local.LocalGlucoseHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FullSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @Suppress("LongLogTag")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val email = prefs.getString("user_email", null)
            ?: return@withContext Result.failure()

        val jsonString: String = try {
            suspendCancellableCoroutine { cont ->
                val url = "${Constants.BASE_URL}/vital/all/$email"
                val req = object : StringRequest(Method.GET, url,
                    { resp -> cont.resume(resp) },
                    { err -> cont.resumeWithException(err) }
                ) {
                    override fun getBodyContentType() = "application/json; charset=utf-8"
                }
                Volley.newRequestQueue(applicationContext).add(req)
            }
        } catch (e: Exception) {
            Log.e("FullSyncWorker", "❌ HTTP/network error", e)
            return@withContext Result.retry()
        }

        return@withContext try {
            val arr = JSONArray(jsonString)
            val rows = mutableListOf<LocalGlucoseHistoryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val raw = obj.optLong("vital_time", -1L)
                if (raw < 0) continue

                val secs = if (raw > 100_000_000_000L) raw / 1000L else raw
                val gv = if (obj.isNull("glucose_value")) null else obj.optDouble("glucose_value", Double.NaN)
                if (gv != null && !gv.isNaN()) {
                    rows += LocalGlucoseHistoryEntry(
                        0,
                        secs.toInt(),
                        gv.toInt()
                    )
                }
            }

            GlucoseDB.getInstance(applicationContext, email)
                .historyDao()
                .replaceAll(rows)

            Log.d("FullSyncWorker", "✅ full sync: ${rows.size} entries")

            // 🔁 Enqueue MonthlyInsightWorker with tag
            val insightWork = OneTimeWorkRequestBuilder<MonthlyInsightWorker>()
                .addTag("monthly_insight_worker_tag")
                .build()
            WorkManager.getInstance(applicationContext).enqueue(insightWork)

            Log.d("FullSyncWorker", "🚀 MonthlyInsightWorker enqueued after sync")

            Result.success()
        } catch (je: JSONException) {
            Log.e("FullSyncWorker", "❌ JSON parse error", je)
            Result.retry()
        } catch (dbEx: Exception) {
            Log.e("FullSyncWorker", "❌ DB write error", dbEx)
            Result.retry()
        }
    }
}
