package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.JarvisAction
import com.example.model.JarvisState
import com.example.model.MessageSender
import com.example.util.ActionExecutor
import com.example.util.JarvisNlpEngine
import com.example.util.SpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JarvisViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(JarvisState())
    val uiState: StateFlow<JarvisState> = _uiState.asStateFlow()

    private var speechManager: SpeechManager? = null

    fun initSpeech(context: Context) {
        if (speechManager == null) {
            speechManager = SpeechManager(context.applicationContext)
        }
    }

    fun toggleTts() {
        _uiState.update { it.copy(ttsEnabled = !it.ttsEnabled) }
    }

    fun toggleAutoExecute() {
        _uiState.update { it.copy(autoExecuteActions = !it.autoExecuteActions) }
    }

    fun processUserCommand(query: String, context: Context) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )

        _uiState.update { state ->
            state.copy(
                isProcessing = true,
                statusText = "PROCESSING COMMAND...",
                messages = state.messages + userMessage
            )
        }

        viewModelScope.launch {
            val apiKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                null
            }

            val result = JarvisNlpEngine.processQuery(trimmed, apiKey)

            val jarvisMessage = ChatMessage(
                sender = MessageSender.JARVIS,
                text = result.replyText,
                jsonPayload = result.jsonPayload,
                action = result.action
            )

            _uiState.update { state ->
                state.copy(
                    isProcessing = false,
                    statusText = if (result.action != null) "ACTION GENERATED FOR BOSS" else "ONLINE - STANDBY FOR BOSS",
                    messages = state.messages + jarvisMessage
                )
            }

            // Speak aloud
            speechManager?.speak(result.replyText, _uiState.value.ttsEnabled)

            // Auto-execute if enabled
            if (_uiState.value.autoExecuteActions && result.action != null) {
                executeAction(jarvisMessage, context)
            }
        }
    }

    fun executeAction(message: ChatMessage, context: Context) {
        val action = message.action ?: return
        viewModelScope.launch {
            val statusMsg = ActionExecutor.execute(context, action) { isFlashOn ->
                _uiState.update { it.copy(isFlashlightOn = isFlashOn) }
            }

            _uiState.update { state ->
                val updatedMessages = state.messages.map { msg ->
                    if (msg.id == message.id) {
                        msg.copy(isExecuted = true, statusMessage = statusMsg)
                    } else {
                        msg
                    }
                }
                state.copy(messages = updatedMessages)
            }
        }
    }

    fun startListening(context: Context, onError: (String) -> Unit) {
        initSpeech(context)
        speechManager?.startListening(
            onResult = { spokenText ->
                processUserCommand(spokenText, context)
            },
            onError = { err ->
                _uiState.update { it.copy(isListening = false, statusText = "ONLINE - STANDBY FOR BOSS") }
                onError(err)
            },
            onListeningStateChanged = { listening ->
                _uiState.update {
                    it.copy(
                        isListening = listening,
                        statusText = if (listening) "LISTENING TO BOSS..." else "ONLINE - STANDBY FOR BOSS"
                    )
                }
            }
        )
    }

    fun stopListening() {
        speechManager?.stopListening()
        _uiState.update { it.copy(isListening = false, statusText = "ONLINE - STANDBY FOR BOSS") }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.shutdown()
    }
}
