package com.example.tfg_beewell_app.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tfg_beewell_app.local.GlucoseDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
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

            val prompt = """
                Analyze the following glucose data from the last 30 days:

                $values

                Provide a short medical-style summary of how the patient's glucose has progressed.
                Mention trends, fluctuations, possible causes, and advice.
            """.trimIndent()

            val summary = ChatGPTClient(applicationContext).getInsight(prompt)

            val prefs = applicationContext.getSharedPreferences("monthly_summary", Context.MODE_PRIVATE)
            prefs.edit().putString("latest_summary", summary).apply()

            Log.d("InsightWorker", "✅ Resumen mensual generado.")
            Result.success()
        } catch (e: Exception) {
            Log.e("InsightWorker", "❌ Error al generar el resumen mensual", e)
            Result.retry()
        }
    }
}
