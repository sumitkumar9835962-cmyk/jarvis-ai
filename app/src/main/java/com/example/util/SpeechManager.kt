package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setPitch(0.85f) // Deep Jarvis voice tone
            tts?.setSpeechRate(1.0f)
            isTtsReady = true
        }
    }

    fun speak(text: String, enabled: Boolean = true) {
        if (!enabled || !isTtsReady) return
        // Do not speak raw JSON brackets aloud if it's pure JSON, speak a brief confirmation or clean JSON summary
        val speakText = if (text.trim().startsWith("{") && text.trim().endsWith("}")) {
            "Action generated for you, Boss."
        } else {
            text
        }
        tts?.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_SPEECH_ID")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningStateChanged: (Boolean) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech Recognition not supported on this device.")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onListeningStateChanged(true)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    onListeningStateChanged(false)
                }

                override fun onError(error: Int) {
                    onListeningStateChanged(false)
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized, Boss."
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue for voice recognition."
                        else -> "Voice recognition notice ($error)."
                    }
                    onError(errorMsg)
                }

                override fun onResults(results: Bundle?) {
                    onListeningStateChanged(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()
                    if (!spokenText.isNullOrBlank()) {
                        onResult(spokenText)
                    } else {
                        onError("Could not understand voice command.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening for Boss...")
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        stopListening()
    }
}
