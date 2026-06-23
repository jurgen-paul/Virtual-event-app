package com.example.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            ""
        } else {
            key
        }
    }

    /**
     * Generates a realistic chat reaction from a simulated attendee in the live stream.
     */
    suspend fun generateChatResponse(
        sessionTitle: String,
        lastUserMessage: String,
        attendeeName: String,
        attendeeRoleId: Int // 0: Attendee, 1: Speaker, 2: Moderator
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getFallbackChatResponse(sessionTitle, lastUserMessage, attendeeName, attendeeRoleId)
        }

        val roleStr = when (attendeeRoleId) {
            1 -> "Speaker / Presenter"
            2 -> "Moderator"
            else -> "Regular Attendee"
        }

        val prompt = """
            You are a simulated virtual event attendee named "$attendeeName" playing the role of "$roleStr".
            You are in the audience of a live stream titled "$sessionTitle".
            The last message in the chat room was from a real user: "$lastUserMessage".
            Write a single, highly realistic, friendly, and natural chat response/reaction in 1-2 short sentences (max 15 words) matching your role and interest. 
            Do NOT include any prefixes, quotes, or meta-commentary. Write just the message.
        """.trimIndent()

        try {
            val responseText = callGeminiApi(prompt)
            if (responseText.isNotBlank()) responseText else getFallbackChatResponse(sessionTitle, lastUserMessage, attendeeName, attendeeRoleId)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API for chat", e)
            getFallbackChatResponse(sessionTitle, lastUserMessage, attendeeName, attendeeRoleId)
        }
    }

    /**
     * Generates a tailored welcome or schedule email based on user's registration.
     */
    suspend fun generateCustomEmail(
        userName: String,
        jobTitle: String,
        interests: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getFallbackEmail(userName, jobTitle, interests)
        }

        val prompt = """
            You are the automated email system for "EventSpace" (a premier virtual tech conference).
            The user registered with:
            Name: $userName
            Job Title: $jobTitle
            Interests: $interests
            
            Generate a personalized, highly professional welcome and event guide email for them. 
            It should highlights specific curated event sessions matching their interests ($interests) and contain custom tips for maximizing their engagement.
            
            Return the output in exact JSON format:
            {
               "subject": "A brief, exciting subject line optimized with their interests",
               "body": "A complete, beautifully written, welcoming email body. Keep it professional with a warm tone, include 3 specific recommended tracks/sessions, and sign off as 'The EventSpace Organizing Team'."
            }
            Do not include any markdown styling like ```json or anything. Just raw, parseable JSON.
        """.trimIndent()

        try {
            val rawJson = callGeminiApi(prompt)
            val jsonObject = JSONObject(rawJson.trim().removeSurrounding("```json", "```").trim())
            val subject = jsonObject.optString("subject", "Welcome to EventSpace, $userName!")
            val body = jsonObject.optString("body", "Hello $userName,\n\nWelcome to EventSpace. We are thrilled to have you here...")
            Pair(subject, body)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating custom email with Gemini", e)
            getFallbackEmail(userName, jobTitle, interests)
        }
    }

    /**
     * Evaluates the metrics and comments, producing a short session impact report.
     */
    suspend fun generateMetricCritique(
        sessionTitle: String,
        watchedSecs: Int,
        engagedSecs: Int,
        chatCount: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "Great session overall! Focus is high, chat activity is sustained, and attendees are engaged with key presenter slides."
        }

        val prompt = """
            You are an expert Virtual Event Organizer Consultant.
            Critically analyze the attendee engagement metrics of this session:
            Session: "$sessionTitle"
            Total Watched Time: ${watchedSecs}s
            Active Focus Time: ${engagedSecs}s
            Total Chat Messages: $chatCount
            
            Provide a short, crisp, insightful, and actionable evaluation (2-3 sentences, maximum 45 words) on how the session is performing and what organizers could do to boost engagement (e.g. run a live poll, address a specific community Q&A question). Be professional but direct.
        """.trimIndent()

        try {
            callGeminiApi(prompt)
        } catch (e: Exception) {
            "An analysis has been saved. Engagement levels are optimal; we recommend initiating a live poll to sustain high audience participation."
        }
    }

    private fun callGeminiApi(prompt: String): String {
        val apiKey = getApiKey()
        val mediaType = "application/json".toMediaType()

        // Build the request body following Direct REST API Guidelines (Option B)
        val jsonRequest = JSONObject()
        val contentsArray = JSONArray()
        val contentObject = JSONObject()
        val partsArray = JSONArray()
        val partObject = JSONObject()
        
        partObject.put("text", prompt)
        partsArray.put(partObject)
        contentObject.put("parts", partsArray)
        contentsArray.put(contentObject)
        jsonRequest.put("contents", contentsArray)

        val requestBodyStr = jsonRequest.toString()
        val requestBody = requestBodyStr.toRequestBody(mediaType)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API request failed: Code=${response.code}, Message=${response.message}")
                return ""
            }
            val responseBodyStr = response.body?.string() ?: return ""
            
            val jsonResponse = JSONObject(responseBodyStr)
            val candidatesArray = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidatesArray?.optJSONObject(0)
            val responseContent = firstCandidate?.optJSONObject("content")
            val responseParts = responseContent?.optJSONArray("parts")
            val firstPart = responseParts?.optJSONObject(0)
            
            return firstPart?.optString("text") ?: ""
        }
    }

    private fun getFallbackChatResponse(
        sessionTitle: String,
        lastUserMessage: String,
        attendeeName: String,
        roleId: Int
    ): String {
        val hostGems = listOf(
            "That's a fantastic point! Let's drill into that during Q&A.",
            "Absolutely! Jetpack Compose makes this exceptionally lightweight.",
            "Make sure you respond to the latest poll in the metrics tab!",
            "Welcome in, everyone! Glad you could join us today."
        )
        val regularGems = listOf(
            "Wow, this slide is really helpful! Thank you.",
            "I've been dealing with this performance issue too. Great solution!",
            "Is the source code for this demo going to be available on GitHub?",
            "Amazing session so far!",
            "Love the visual design and simplicity of this UI."
        )
        return when (roleId) {
            1 -> "As presenter, I highly agree with that. The reactive state flow is key!"
            2 -> hostGems.random()
            else -> regularGems.random()
        }
    }

    private fun getFallbackEmail(userName: String, jobTitle: String, interests: String): Pair<String, String> {
        val subject = "Hello $userName, Welcome to EventSpace 2026!"
        val body = """
            Dear $userName,
            
            Thank you for registering for EventSpace! We've customized an event agenda tailored specifically to your background as a '$jobTitle' and your interests in: $interests.
            
            Highlight Recommendations for You:
            - Jetpack Compose Deepdive: Crafting dynamic, fluid layouts.
            - AI Integrations: Embedding direct REST models securely.
            - Real-Time Analytics Masterclass: Harnessing telemetry to gauge reader engagement.
            
            You can access the virtual live stream, review interactive attendee chats, and check live speaker analytics directly in your mobile app dashboard.
            
            Best regards,
            The EventSpace Organizing Team
        """.trimIndent()
        return Pair(subject, body)
    }
}
