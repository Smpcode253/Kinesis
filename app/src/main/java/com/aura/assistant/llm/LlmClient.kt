package com.aura.assistant.llm

import android.util.Log
import com.aura.assistant.BuildConfig
import com.aura.assistant.data.db.dao.ConversationHistoryDao
import com.aura.assistant.data.db.entities.ConversationEntry
import com.aura.assistant.domain.model.AuraIntent
import com.aura.assistant.domain.model.UserContext
import com.aura.assistant.llm.model.LlmMessage
import com.aura.assistant.llm.model.LlmRequest
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Client that communicates with the LLM API to parse natural language into
 * structured [AuraIntent] objects and generate conversational responses.
 */
class LlmClient(
    private val conversationHistoryDao: ConversationHistoryDao,
    private val baseUrl: String = "https://api.openai.com/",
    private val model: String = "gpt-4o-mini"
) {

    private val gson = Gson()

    private val retrofit: Retrofit by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api: LlmApiService by lazy {
        retrofit.create(LlmApiService::class.java)
    }

    private val systemPrompt = """
        You are Aura, a proactive and context-aware voice assistant running on an Android device.
        Your primary goal is to help the user manage communications, navigation, and tasks safely,
        especially while driving.

        You must parse the user's natural language input and respond with a JSON object representing
        the user's intent. The JSON must have an "intent" field with one of these values:
        - "make_call" (fields: contact_name, phone_number)
        - "send_message" (fields: contact_name, phone_number, message)
        - "navigate" (fields: destination, mode)
        - "create_reminder" (fields: title, description, trigger_time_ms)
        - "query_agenda" (fields: date)
        - "add_calendar_event" (fields: title, description, start_time_ms, end_time_ms, location)
        - "query_context" (no extra fields)
        - "general_response" (fields: text)
        - "unknown" (fields: raw_text)

        Be concise, safe, and helpful. Prioritize hands-free safety when the context is "Driving".
        Always respond with valid JSON only — no markdown, no explanation, just JSON.
    """.trimIndent()

    /**
     * Parses a user utterance into a structured [AuraIntent].
     * Also saves the conversation entry to the local database.
     */
    suspend fun parseIntent(userText: String, context: UserContext): AuraIntent {
        val apiKey = BuildConfig.LLM_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "LLM_API_KEY not set — returning unknown intent")
            return AuraIntent.Unknown(userText)
        }

        val recentHistory = conversationHistoryDao.getRecentHistory(10)
        val messages = mutableListOf<LlmMessage>()
        messages.add(LlmMessage("system", "$systemPrompt\n\nCurrent user context: ${context.label}"))
        recentHistory.reversed().forEach {
            messages.add(LlmMessage(it.role, it.content))
        }
        messages.add(LlmMessage("user", userText))

        return try {
            val request = LlmRequest(model = model, messages = messages)
            val response = api.getChatCompletion("Bearer $apiKey", request)
            val rawJson = response.choices.firstOrNull()?.message?.content ?: "{}"

            conversationHistoryDao.insertEntry(
                ConversationEntry(role = "user", content = userText, contextTag = context.label)
            )
            conversationHistoryDao.insertEntry(
                ConversationEntry(role = "assistant", content = rawJson, contextTag = context.label)
            )

            parseJsonToIntent(rawJson, userText)
        } catch (e: Exception) {
            Log.e(TAG, "LLM request failed", e)
            AuraIntent.Unknown(userText)
        }
    }

    /**
     * Generates a plain conversational response without strict intent parsing.
     */
    suspend fun generateResponse(prompt: String, context: UserContext): String {
        val apiKey = BuildConfig.LLM_API_KEY
        if (apiKey.isBlank()) return "I'm sorry, I'm not configured yet. Please set your API key."

        val messages = listOf(
            LlmMessage("system", "You are Aura, a helpful voice assistant. Current context: ${context.label}"),
            LlmMessage("user", prompt)
        )
        return try {
            val request = LlmRequest(model = model, messages = messages, maxTokens = 256)
            val response = api.getChatCompletion("Bearer $apiKey", request)
            response.choices.firstOrNull()?.message?.content
                ?: "I couldn't generate a response at this time."
        } catch (e: Exception) {
            Log.e(TAG, "LLM response generation failed", e)
            "I'm having trouble connecting right now. Please try again."
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJsonToIntent(json: String, originalText: String): AuraIntent {
        return try {
            val map = gson.fromJson(json, Map::class.java) as Map<String, Any?>
            when (val intent = map["intent"] as? String) {
                "make_call" -> AuraIntent.MakeCall(
                    contactName = map["contact_name"] as? String,
                    phoneNumber = map["phone_number"] as? String
                )
                "send_message" -> AuraIntent.SendMessage(
                    contactName = map["contact_name"] as? String,
                    phoneNumber = map["phone_number"] as? String,
                    message = map["message"] as? String ?: ""
                )
                "navigate" -> AuraIntent.Navigate(
                    destination = map["destination"] as? String ?: "",
                    mode = map["mode"] as? String ?: "driving"
                )
                "create_reminder" -> AuraIntent.CreateReminder(
                    title = map["title"] as? String ?: "",
                    description = map["description"] as? String ?: "",
                    triggerTimeMs = (map["trigger_time_ms"] as? Double)?.toLong()
                        ?: System.currentTimeMillis() + 3_600_000L
                )
                "query_agenda" -> AuraIntent.QueryAgenda(
                    date = map["date"] as? String
                )
                "add_calendar_event" -> AuraIntent.AddCalendarEvent(
                    title = map["title"] as? String ?: "",
                    description = map["description"] as? String ?: "",
                    startTimeMs = (map["start_time_ms"] as? Double)?.toLong()
                        ?: System.currentTimeMillis(),
                    endTimeMs = (map["end_time_ms"] as? Double)?.toLong()
                        ?: System.currentTimeMillis() + 3_600_000L,
                    location = map["location"] as? String ?: ""
                )
                "query_context" -> AuraIntent.QueryContext
                "general_response" -> AuraIntent.GeneralResponse(
                    text = map["text"] as? String ?: ""
                )
                else -> {
                    Log.w(TAG, "Unknown intent type from LLM: $intent")
                    AuraIntent.Unknown(originalText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse LLM JSON response", e)
            AuraIntent.Unknown(originalText)
        }
    }

    companion object {
        private const val TAG = "LlmClient"
    }
}
