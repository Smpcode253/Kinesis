package com.aura.assistant.llm

import com.aura.assistant.llm.model.LlmRequest
import com.aura.assistant.llm.model.LlmResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service interface for the LLM API (OpenAI-compatible endpoint).
 */
interface LlmApiService {

    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: LlmRequest
    ): LlmResponse
}
