package com.example.tfg_beewell_app.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking      // ← IMPORTANTE
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

object HealthReader {

    /**
     *  Versión suspend (se llama desde Kotlin con coroutines)
     *  Devuelve la última FC registrada en Health Connect o null.
     */
    suspend fun getLastHeartRateBpm(context: Context): Long? =
        withContext(Dispatchers.IO) {

            // 1) Cliente HC  (puede lanzar excepción si la app no está instalada)
            val client = try {
                HealthConnectClient.getOrCreate(context)
            } catch (_: Throwable) {
                return@withContext null
            }

            // 2) Rango de búsqueda: última hora
            val end   = Instant.now()
            val start = end.minus(1, ChronoUnit.HOURS)

            // 3) Leer máx. 100 registros
            val page = client.readRecords(
                ReadRecordsRequest(
                    recordType      = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageSize        = 100
                )
            )

            // 4) Registro más reciente
            val latest = page.records.maxByOrNull { it.startTime } ?: return@withContext null

            // 5) Última muestra dentro del registro
            latest.samples.lastOrNull()?.beatsPerMinute        // ← Long?
        }

    /**
     *  Versión bloqueante para poder llamarla cómodamente desde Java.
     */
    @JvmStatic
    fun getLastHeartRateBpmBlocking(context: Context): Long? =
        runBlocking { getLastHeartRateBpm(context) }
}
