package com.aiassistant.app

import android.graphics.Bitmap
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object AIClient {

    // ضع مفتاح Gemini الجديد هنا
    private const val API_KEY = "ضع_مفتاح_Gemini_هنا"

    private const val API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun ask(prompt: String): String {
        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }
        return sendRequest(json)
    }

    fun askWithImage(bitmap: Bitmap, prompt: String): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }
        return sendRequest(json)
    }

    private fun sendRequest(json: JSONObject): String {
        if (API_KEY.startsWith("ضع_")) {
            return "ضع مفتاح Gemini في AIClient.kt أولاً."
        }

        val url = "$API_URL?key=$API_KEY"
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return "Empty response"
            val jsonObj = JSONObject(responseBody)

            if (jsonObj.has("candidates")) {
                val parts = jsonObj.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                parts.getJSONObject(0).getString("text")
            } else if (jsonObj.has("error")) {
                "Error: " + jsonObj.getJSONObject("error").getString("message")
            } else {
                "Response: $responseBody"
            }
        } catch (e: IOException) {
            "Network error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
