package com.aura.assistant.llm.model

import com.google.gson.annotations.SerializedName

/**
 * Response from the LLM API (OpenAI-compatible format).
 */
data class LlmResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("object")
    val objectType: String,
    @SerializedName("choices")
    val choices: List<LlmChoice>,
    @SerializedName("usage")
    val usage: LlmUsage?
)

data class LlmChoice(
    @SerializedName("index")
    val index: Int,
    @SerializedName("message")
    val message: LlmMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class LlmUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)
