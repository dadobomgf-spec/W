package com.aiassistant.app

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AIClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // ⚠️ ضع مفتاحك الجديد هنا
    private const val API_KEY = "sk-or-v1-ضع_مفتاحك_هنا"
    private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val MODEL = "qwen/qwen-2.5-7b-instruct:free"

    fun ask(prompt: String): String {
        val json = JSONObject().apply {
            put("model", MODEL)
            put("stream", false)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a helpful AI assistant on Android. Answer briefly.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return "Empty response"
            val jsonObj = JSONObject(responseBody)
            if (jsonObj.has("choices")) {
                jsonObj.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } else if (jsonObj.has("error")) {
                "Error: ${jsonObj.getJSONObject("error").getString("message")}"
            } else {
                "Error: $responseBody"
            }
        } catch (e: IOException) {
            "Network error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
