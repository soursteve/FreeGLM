package com.freeglm.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeglm.app.data.ChatMessage
import com.freeglm.app.data.ChatRepository
import com.freeglm.app.data.Role
import com.freeglm.app.data.UiMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    fun onInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(input = value)
    }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.isSending) return

        val history = _uiState.value.messages
        val userMessage = UiMessage(role = Role.USER, text = text)
        val assistantMessage = UiMessage(role = Role.ASSISTANT, text = "", isStreaming = true)

        _uiState.value = _uiState.value.copy(
            messages = history + userMessage + assistantMessage,
            input = "",
            isSending = true,
            error = null
        )

        val requestMessages = (history + userMessage).map {
            ChatMessage(role = if (it.role == Role.USER) "user" else "assistant", content = it.text)
        }

        streamJob = viewModelScope.launch {
            repository.streamChat(requestMessages)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Something went wrong",
                        isSending = false
                    )
                    updateLastAssistantMessage(isStreaming = false)
                }
                .onCompletion { cause ->
                    if (cause == null) {
                        _uiState.value = _uiState.value.copy(isSending = false)
                        updateLastAssistantMessage(isStreaming = false)
                    }
                }
                .collect { delta ->
                    appendToLastAssistantMessage(delta)
                }
        }
    }

    private fun appendToLastAssistantMessage(delta: String) {
        val current = _uiState.value.messages
        if (current.isEmpty()) return
        val lastIndex = current.lastIndex
        val updated = current.toMutableList()
        val last = updated[lastIndex]
        updated[lastIndex] = last.copy(text = last.text + delta)
        _uiState.value = _uiState.value.copy(messages = updated)
    }

    private fun updateLastAssistantMessage(isStreaming: Boolean) {
        val current = _uiState.value.messages
        if (current.isEmpty()) return
        val lastIndex = current.lastIndex
        val updated = current.toMutableList()
        updated[lastIndex] = updated[lastIndex].copy(isStreaming = isStreaming)
        _uiState.value = _uiState.value.copy(messages = updated)
    }

    fun newChat() {
        streamJob?.cancel()
        _uiState.value = ChatUiState()
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}
