package com.example.model

enum class MessageSender {
    USER, JARVIS
}

sealed class JarvisAction {
    data class YouTube(val query: String) : JarvisAction()
    data class WhatsApp(val contact: String, val message: String) : JarvisAction()
    object Camera : JarvisAction()
    object FlashlightOn : JarvisAction()
    object FlashlightOff : JarvisAction()
    data class Music(val query: String) : JarvisAction()
    data class OpenApp(val appName: String) : JarvisAction()
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val jsonPayload: String? = null,
    val action: JarvisAction? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isExecuted: Boolean = false,
    val statusMessage: String? = null
)

data class JarvisState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val ttsEnabled: Boolean = true,
    val autoExecuteActions: Boolean = true,
    val statusText: String = "JARVIS ONLINE - STANDBY FOR BOSS",
    val messages: List<ChatMessage> = emptyList()
)
