package com.antigravity.writingassistant

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GeminiClient {
    private val client = OkHttpClient()
    private val apiKey = BuildConfig.GEMINI_API_KEY // TODO: Use BuildConfig or User Preference

    data class PromptConfig(
        val originalText: String,
        val instruction: String? = null,
        val contextBefore: String? = null,
        val contextAfter: String? = null
    )

    fun generateContent(config: PromptConfig, callback: (String?) -> Unit) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemma-3-27b-it:generateContent?key=$apiKey"
        
        val systemPrompt = """You are an expert writing assistant that rewrites text. CRITICAL RULES:
1. You MUST rewrite the text according to the instruction given.
2. Output ONLY the rewritten text - no explanations, no quotes, no prefixes like "Here's", no "Sure!", nothing else.
3. Keep the rewritten text similar in length to the original unless asked to make it concise.
4. Do NOT repeat the original text - transform it.
5. Preserve the core meaning while applying the requested style/improvement."""
        
        val userPromptBuilder = StringBuilder()
        
        if (!config.instruction.isNullOrEmpty()) {
            userPromptBuilder.append("INSTRUCTION: ${config.instruction}\n\n")
        }
        
        userPromptBuilder.append("TEXT TO REWRITE:\n${config.originalText}\n\n")
        userPromptBuilder.append("REWRITTEN TEXT:")

        val finalPrompt = userPromptBuilder.toString()
        
        val jsonBody = JSONObject()
        val contents = JSONArray()
        val contentPart = JSONObject()
        val parts = JSONArray()
        
        // Add System Instruction if supported by API (Flash supports 'systemInstruction', but let's keep it simple in user prompt for now to match v1beta structure easily)
        // Actually, for better results, let's prepend system prompt to text part
        val textPart = JSONObject()
        textPart.put("text", "$systemPrompt\n\n$finalPrompt")
        
        parts.put(textPart)
        contentPart.put("parts", parts)
        contents.put(contentPart)
        jsonBody.put("contents", contents)

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val result = parseGeminiResponse(responseBody)
                    callback(result)
                } else {
                    android.util.Log.e("GeminiClient", "Error: ${response.code} ${response.message}")
                    callback(null)
                }
            }
        })
    }
    
    private fun parseGeminiResponse(json: String?): String? {
        if (json == null) return null
        try {
            val root = JSONObject(json)
            if (root.has("candidates")) {
                val candidates = root.getJSONArray("candidates")
                if (candidates.length() > 0) {
                     val content = candidates.getJSONObject(0).getJSONObject("content")
                     val parts = content.getJSONArray("parts")
                     if (parts.length() > 0) {
                         return parts.getJSONObject(0).getString("text")
                     }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
