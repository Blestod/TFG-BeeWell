package com.example.tfg_beewell_app.ui.home

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tfg_beewell_app.Constants
import com.example.tfg_beewell_app.ui.VitalData
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "HomeViewModel"
private const val INSIGHTS_URL =  Constants.BASE_URL + "/generate_insights";

data class Insights(
    val vitalsRecommendation: String,
    val predictionRecommendation: String
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val _insights = MutableLiveData<Insights>()
    val insights: LiveData<Insights> = _insights

    private var lastFetch = 0L
    private val prefs     = app.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    private val userEmail = prefs.getString("user_email", "") ?: ""

    fun fetchInsightsIfNeeded(
        vitals: VitalData,
        predictions: List<Entry>
    ) {
        val now = System.currentTimeMillis()
        Log.d(TAG, "fetchInsightsIfNeeded called — now=$now, lastFetch=$lastFetch")
        if (now - lastFetch < TimeUnit.HOURS.toMillis(1)) {
            Log.d(TAG, "skip fetch — aún no ha pasado 1h")
            return
        }
        lastFetch = now

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) JSON de vitals
                val vitalsJson = JSONObject().apply {
                    vitals.glucoseValue?.let       { put("glucose", it) }
                    vitals.heartRate?.toInt()?.let { put("heart_rate", it) }
                    vitals.temperature?.let        { put("temperature", it) }
                    vitals.oxygenSaturation?.let   { put("oxygen_saturation", it) }
                }
                // 2) JSON array de predicciones
                val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                val predsArr = JSONArray().also { arr ->
                    predictions.forEach { e ->
                        arr.put(JSONObject().apply {
                            put("time",  fmt.format(Date((e.x * 1000).toLong())))
                            put("value", e.y)
                        })
                    }
                }
                // 3) cuerpo completo
                val body = JSONObject().apply {
                    put("user_email",   userEmail)
                    put("vitals",       vitalsJson)
                    put("predictions",  predsArr)
                }
                Log.d(TAG, "request JSON → $body")

                // 4) OkHttp request con logs de respuesta
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val reqBody   = RequestBody.create(mediaType, body.toString())
                val req = Request.Builder()
                    .url(INSIGHTS_URL)
                    .header("User-Agent", "Mozilla/5.0") // a veces necesario
                    .post(reqBody)
                    .build()

                OkHttpClient().newCall(req).execute().use { resp ->
                    val code = resp.code
                    val ct   = resp.header("Content-Type")
                    val raw  = resp.body?.string().orEmpty()
                    Log.d(TAG, "HTTP $code  Content-Type: $ct")
                    Log.d(TAG, "RAW RESPONSE:\n$raw")

                    if (code == 200 && ct?.contains("application/json")==true) {
                        val js = JSONObject(raw)
                        val ins = Insights(
                            js.optString("recommendation_vitals","(no vitals)"),
                            js.optString("recommendation_prediction","(no pred)")
                        )
                        Log.d(TAG, "parsed insights → $ins")
                        _insights.postValue(ins)
                    } else {
                        Log.e(TAG, "Respuesta no-JSON o código !=200 → omito parsear")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "error fetching insights", e)
            }
        }
    }
}
