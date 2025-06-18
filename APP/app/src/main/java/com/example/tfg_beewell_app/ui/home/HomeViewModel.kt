package com.example.tfg_beewell_app.ui.home

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.tfg_beewell_app.ui.VitalData
import com.example.tfg_beewell_app.utils.HomeService
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "HomeViewModel"

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
        if (now - lastFetch < TimeUnit.MINUTES.toMillis(30)) return
        lastFetch = now

        // 1️⃣ Build the inner 'health_data' object
        val vitalsJson = JSONObject().apply {
            vitals.glucoseValue?.let       { put("glucose", it) }
            vitals.heartRate?.toInt()?.let { put("heart_rate", it) }
            vitals.temperature?.let        { put("temperature", it) }
            vitals.oxygenSaturation?.let   { put("oxygen_saturation", it) }
        }

        // 2️⃣ Build the 'events' array
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val predsArr = JSONArray().also { arr ->
            predictions.forEach { e ->
                arr.put(JSONObject().apply {
                    put("time",  fmt.format(Date((e.x * 1000).toLong())))
                    put("value", e.y)
                })
            }
        }

        // 3️⃣ Assemble the 'patient_data' object
        val patientData = JSONObject().apply {
            put("user_email",  userEmail)
            put("health_data", vitalsJson)
            put("events",      predsArr)
        }

        // 4️⃣ Wrap it under the top-level key
        val wrapper = JSONObject().apply {
            put("patient_data", patientData)
        }

        // 5️⃣ Fire the call to /api/home
        viewModelScope.launch(Dispatchers.IO) {
            HomeService
                .getInstance()
                .fetchHomeInsights(wrapper, object: HomeService.HomeCallback {
                    override fun onResult(
                        vitalsRec: String,
                        predictionRec: String
                    ) {
                        Log.d(TAG, "Home API → vitalsRec='$vitalsRec', predRec='$predictionRec'")
                        _insights.postValue(
                            Insights(
                                vitalsRecommendation    = vitalsRec,
                                predictionRecommendation = predictionRec
                            )
                        )
                    }
                })
        }
    }
}
