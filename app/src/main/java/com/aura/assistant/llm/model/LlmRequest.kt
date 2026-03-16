package com.aura.assistant.llm.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for the LLM API (OpenAI-compatible format).
 */
data class LlmRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<LlmMessage>,
    @SerializedName("temperature")
    val temperature: Double = 0.2,
    @SerializedName("max_tokens")
    val maxTokens: Int = 512
)

data class LlmMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)
