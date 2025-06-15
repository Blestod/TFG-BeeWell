// RecoWorker.kt
package com.example.tfg_beewell_app.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.tfg_beewell_app.utils.RecommendationService
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RecoWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    companion object {
        const val PREFS = "reco_prefs"
        const val KEY_DIET = "diet_txt"
        const val KEY_EXER = "exer_txt"
        const val BROADCAST = "com.example.tfg_beewell_app.RECO_UPDATED"
        private const val BACKEND = "https://beewell.blestod.com"
    }

    override fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userEmail = prefs.getString("user_email", null)
            Log.d("RecoWorker", "📩 Email in prefs: $userEmail")
            if (userEmail.isNullOrEmpty()) {
                Log.e("RecoWorker", "❌ No se encontró el email del usuario.")
                return Result.failure()
            }

            val meals = fetchJsonArray("$BACKEND/meal_recommendation_data/$userEmail")
            val insulin = wrapObject(fetchJson("$BACKEND/insulin_recommendation_data/$userEmail"))
            val exercise = fetchJsonArray("$BACKEND/exercise_recommendation_data/$userEmail")
            val health = fetchJson("$BACKEND/user/static/$userEmail")

            val events = JSONArray().apply {
                appendAll(this, meals)
                appendAll(this, insulin)
                appendAll(this, exercise)
            }

            val patientData = JSONObject().apply {
                put("events", events)
                put("health_data", health)
            }

            Log.d("RecoWorker", "📦 Sending data to AI:\n$patientData")
            Log.d("RecoWorker", "🚀 Sending recommendation request to Sciling AI")


            var dietResult = ""
            var exerResult = ""

            val latch = java.util.concurrent.CountDownLatch(1)

            RecommendationService.getInstance()
                .fetchReco(patientData) { diet, exercise ->
                    dietResult = diet
                    exerResult = exercise
                    latch.countDown()
                }

            latch.await()

            val sp: SharedPreferences = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            sp.edit()
                .putString(KEY_DIET, dietResult)
                .putString(KEY_EXER, exerResult)
                .apply()

            val intent = Intent(BROADCAST)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun fetchJsonArray(url: String): JSONArray {
        val json = fetchRaw(url)
        return JSONArray(json)
    }

    private fun fetchJson(url: String): JSONObject {
        val json = fetchRaw(url)
        return JSONObject(json)
    }

    private fun fetchRaw(urlStr: String): String {
        Log.d("RecoWorker", "📡 Request to: $urlStr")

        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"

        val code = conn.responseCode
        return if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val errorBody = conn.errorStream?.bufferedReader()?.readText()
            Log.e("RecoWorker", "❌ Error fetching $urlStr (code $code): $errorBody")
            throw RuntimeException("Failed request to $urlStr")
        }
    }



    private fun appendAll(target: JSONArray, source: JSONArray) {
        for (i in 0 until source.length()) {
            target.put(source.get(i))
        }
    }

    private fun wrapObject(obj: JSONObject?): JSONArray {
        return JSONArray().apply {
            if (obj != null && obj.length() > 0) put(obj)
        }
    }
}