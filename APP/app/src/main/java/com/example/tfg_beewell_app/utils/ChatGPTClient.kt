package com.example.tfg_beewell_app.utils

import android.content.Context
import android.util.Log
import com.example.tfg_beewell_app.Constants
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ChatGPTClient(private val context: Context) {
    private val client = OkHttpClient()

    fun getInsight(prompt: String): String {
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val json = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", messagesArray)
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${Constants.OPENAI_API_KEY}")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("ChatGPT", "❌ API error ${response.code}:\n$errorBody")
                    return "Error al obtener observación."
                }

                val resultJson = JSONObject(response.body?.string() ?: "")
                val text = resultJson
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                text.trim()
            }
        } catch (e: Exception) {
            Log.e("ChatGPT", "❌ Error de red/API", e)
            "Error al contactar ChatGPT."
        }
    }
}
