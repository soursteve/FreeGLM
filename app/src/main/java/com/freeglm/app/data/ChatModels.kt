package com.freeglm.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val messages: List<ChatMessage>
)

@Serializable
data class ChatChunk(
    val content: String? = null,
    @SerialName("error")
    val error: String? = null
)

enum class Role { USER, ASSISTANT }

data class UiMessage(
    val role: Role,
    val text: String,
    val isStreaming: Boolean = false
)
