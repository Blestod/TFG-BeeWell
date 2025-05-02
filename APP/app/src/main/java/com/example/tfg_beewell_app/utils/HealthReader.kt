// utils/HealthReader.kt
package com.example.tfg_beewell_app.utils

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord

import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.tfg_beewell_app.ui.VitalData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/** pequeño contenedor con la última muestra */
data class HeartSample(
    val bpm : Long,        // latidos-por-minuto
    val time: Instant      // instante de esa muestra
)

object HealthReader {

    /** →  nuevo método “completo” (suspend) */
    suspend fun getLastHeartRate(context: Context): HeartSample? =
        withContext(Dispatchers.IO) {

            val client = try {                        // 1 cliente
                HealthConnectClient.getOrCreate(context)
            } catch (_: Throwable) { return@withContext null }

            val end   = Instant.now()                 // 2 rango 1 h
            val start = end.minus(1, ChronoUnit.HOURS)

            val page  = client.readRecords(           // 3 consulta
                ReadRecordsRequest(
                    recordType      = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageSize        = 100
                )
            )

            val latest = page.records.maxByOrNull { it.startTime } ?: return@withContext null
            val sample = latest.samples.maxByOrNull { it.time }        ?: return@withContext null

            HeartSample(sample.beatsPerMinute, sample.time)           // ← resultado
        }

    /** →  versión “solo bpm”  */
    suspend fun getLastHeartRateBpm(context: Context): Long? =
        getLastHeartRate(context)?.bpm

    /** bloqueante para Java */
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

            // Heart rate
            Log.d("HealthReader", "🔍 Leyendo frecuencia cardíaca...")
            val hrRecords = client.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records
            hrRecords.maxByOrNull { it.startTime }?.let { rec ->
                val bpm = rec.samples.maxByOrNull { it.time }?.beatsPerMinute
                result.setHeartRate(bpm?.toFloat())
                Log.d("HealthReader", "❤️ Heart rate: $bpm")
            }

            // Glucose
            Log.d("HealthReader", "🔍 Leyendo glucosa...")
            val glucose = client.readRecords(
                ReadRecordsRequest(BloodGlucoseRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records.lastOrNull()
            glucose?.let {
                result.setGlucoseValue(it.level.inMillimolesPerLiter.toInt())
                Log.d("HealthReader", "🩸 Glucosa: ${it.level.inMillimolesPerLiter}")
            }

            // Temperature
            Log.d("HealthReader", "🔍 Leyendo temperatura corporal...")
            val temp = client.readRecords(
                ReadRecordsRequest(BodyTemperatureRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records.lastOrNull()
            temp?.let {
                result.setTemperature(it.temperature.inCelsius.toFloat())
                Log.d("HealthReader", "🌡️ Temperatura: ${it.temperature.inCelsius}")
            }

            // Calories
            Log.d("HealthReader", "🔍 Leyendo calorías quemadas...")
            val calories = client.readRecords(
                ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records.sumOf { it.energy.inKilocalories }
            result.setCalories(calories.toFloat())
            Log.d("HealthReader", "🔥 Calorías: $calories")

            // Oxygen saturation
            Log.d("HealthReader", "🔍 Leyendo saturación de oxígeno...")
            val oxygen = client.readRecords(
                ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(oneHourAgo, now))
            ).records.lastOrNull()
            oxygen?.let {
                result.setOxygenSaturation(it.percentage.value.toFloat())
                Log.d("HealthReader", "🫁 Saturación: ${it.percentage.value}%")
            }

// Sleep session duration
            Log.d("HealthReader", "🔍 Leyendo sesión de sueño...")
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
