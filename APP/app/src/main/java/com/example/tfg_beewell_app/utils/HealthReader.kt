package com.example.tfg_beewell_app.utils

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.tfg_beewell_app.ui.VitalData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

data class HeartSample(
    val bpm: Long,
    val time: Instant
)

object HealthReader {

    suspend fun getLastHeartRate(context: Context): HeartSample? = withContext(Dispatchers.IO) {
        val client = try {
            HealthConnectClient.getOrCreate(context)
        } catch (_: Throwable) { return@withContext null }

        val end = Instant.now()
        val start = end.minus(1, ChronoUnit.HOURS)

        val page = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageSize = 100
            )
        )

        val latest = page.records.maxByOrNull { it.startTime } ?: return@withContext null
        val sample = latest.samples.maxByOrNull { it.time } ?: return@withContext null

        HeartSample(sample.beatsPerMinute, sample.time)
    }

    suspend fun getLastHeartRateBpm(context: Context): Long? =
        getLastHeartRate(context)?.bpm

    @JvmStatic
    fun getLastHeartRateBpmBlocking(context: Context): Long? =
        runBlocking { getLastHeartRateBpm(context) }

    suspend fun getLatestVitals(context: Context, email: String): VitalData? = withContext(Dispatchers.IO) {
        val client = try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.e("HealthReader", "❌ Error al obtener cliente de HealthConnect: ${e.message}")
            return@withContext null
        }

        val now = Instant.now()
        val oneHourAgo = now.minus(1, ChronoUnit.HOURS)

        val result = VitalData().apply {
            setUserEmail(email)
        }

        try {
            Log.d("HealthReader", "📡 Buscando datos desde $oneHourAgo hasta $now")

            val hrRecords = client.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records
            hrRecords.maxByOrNull { it.startTime }?.let { rec ->
                val bpm = rec.samples.maxByOrNull { it.time }?.beatsPerMinute
                result.setHeartRate(bpm?.toFloat())
                Log.d("HealthReader", "❤️ Heart rate: $bpm")
            }

            // ✅ Use user-scoped glucose cache
            val session = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val user = session.getString("user_email", null)
            val gPrefs = context.getSharedPreferences("glucose_cache_$user", Context.MODE_PRIVATE)
            val glucoseValue = gPrefs.getInt("glucose_mgdl", -1)
            if (glucoseValue != -1) {
                result.setGlucoseValue(glucoseValue)
                Log.d("HealthReader", "🩸 Glucosa desde Receiver: $glucoseValue mg/dL")
                gPrefs.edit().remove("glucose_mgdl").apply()
            } else {
                val glucose = client.readRecords(
                    ReadRecordsRequest(BloodGlucoseRecord::class, TimeRangeFilter.between(oneHourAgo, now))
                ).records.lastOrNull()
                glucose?.let {
                    result.setGlucoseValue(it.level.inMillimolesPerLiter.toInt())
                    Log.d("HealthReader", "🩸 Glucosa desde HC: ${it.level.inMillimolesPerLiter}")
                }
            }

            val temp = client.readRecords(
                ReadRecordsRequest(BodyTemperatureRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records.lastOrNull()
            temp?.let {
                result.setTemperature(it.temperature.inCelsius.toFloat())
                Log.d("HealthReader", "🌡️ Temperatura: ${it.temperature.inCelsius}")
            }

            val calories = client.readRecords(
                ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records.sumOf { it.energy.inKilocalories }
            result.setCalories(calories.toFloat())
            Log.d("HealthReader", "🔥 Calorías: $calories")

            val oxygen = client.readRecords(
                ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records.lastOrNull()
            oxygen?.let {
                result.setOxygenSaturation(it.percentage.value.toFloat())
                Log.d("HealthReader", "🫁 Saturación: ${it.percentage.value}%")
            }

            val sleep = client.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records.lastOrNull()
            sleep?.let {
                val duration = Duration.between(it.startTime, it.endTime).toMinutes()
                result.setSleepDuration(duration.toInt())
                Log.d("HealthReader", "🛌 Duración del sueño: $duration minutos")
            }

        } catch (e: Exception) {
            Log.e("HealthReader", "❌ Error al leer datos: ${e.message}", e)
            return@withContext null
        }

        Log.d("HealthReader", "✅ Resultado final de vitales: $result")
        return@withContext result
    }

    @JvmStatic
    fun getLatestVitalsBlocking(context: Context, email: String): VitalData? =
        runBlocking { getLatestVitals(context, email) }
}
