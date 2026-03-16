package com.aura.assistant.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale
import java.util.UUID

/**
 * Manages both Voice-to-Text (VTT/STT) and Text-to-Speech (TTS) functionality.
 * Uses the Android built-in SpeechRecognizer and TextToSpeech engines for
 * on-device processing without sending audio data to external servers.
 */
class SpeechManager(private val context: Context) {

    // ─── TTS ──────────────────────────────────────────────────────────────────

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _ttsEvents = Channel<TtsEvent>(Channel.BUFFERED)
    val ttsEvents: Flow<TtsEvent> = _ttsEvents.receiveAsFlow()

    fun initTts(onReady: (() -> Unit)? = null) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.getDefault())
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA
                            && result != TextToSpeech.LANG_NOT_SUPPORTED
                    if (isTtsReady) {
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _ttsEvents.trySend(TtsEvent.SpeechStarted(utteranceId ?: ""))
                            }

                            override fun onDone(utteranceId: String?) {
                                _ttsEvents.trySend(TtsEvent.SpeechCompleted(utteranceId ?: ""))
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _ttsEvents.trySend(TtsEvent.SpeechError(utteranceId ?: "", "TTS error"))
                            }
                        })
                        onReady?.invoke()
                    }
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    /**
     * Speaks the given text aloud. Stops any currently playing speech first.
     * @return The utterance ID for tracking.
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH): String {
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready, cannot speak: $text")
            return ""
        }
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(text, queueMode, params, utteranceId)
        return utteranceId
    }

    /** Stops any currently playing speech. */
    fun stopSpeaking() {
        tts?.stop()
    }

    /** Returns true if TTS is currently speaking. */
    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    // ─── STT (Voice-to-Text) ─────────────────────────────────────────────────

    private var speechRecognizer: SpeechRecognizer? = null
    private val _sttEvents = Channel<SttEvent>(Channel.BUFFERED)
    val sttEvents: Flow<SttEvent> = _sttEvents.receiveAsFlow()

    /**
     * Initialises the speech recognizer. Must be called on the main thread.
     */
    fun initStt() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition is not available on this device.")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _sttEvents.trySend(SttEvent.ReadyForSpeech)
            }

            override fun onBeginningOfSpeech() {
                _sttEvents.trySend(SttEvent.SpeechBegun)
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changes — not forwarded to reduce noise
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _sttEvents.trySend(SttEvent.SpeechEnded)
            }

            override fun onError(error: Int) {
                val msg = sttErrorMessage(error)
                Log.e(TAG, "STT error: $msg ($error)")
                _sttEvents.trySend(SttEvent.Error(error, msg))
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: return
                if (matches.isNotEmpty()) {
                    _sttEvents.trySend(SttEvent.Result(matches[0], matches))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                _sttEvents.trySend(SttEvent.PartialResult(partial))
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    /**
     * Starts listening for user speech. The result is emitted as [SttEvent.Result].
     */
    fun startListening(language: String = Locale.getDefault().toLanguageTag()) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    /** Stops listening. */
    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    /** Cancels the current recognition session. */
    fun cancelListening() {
        speechRecognizer?.cancel()
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /** Releases all resources. Call this when the owning service/activity is destroyed. */
    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        _ttsEvents.close()
        _sttEvents.close()
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun sttErrorMessage(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Unknown error"
    }

    companion object {
        private const val TAG = "SpeechManager"
    }
}

// ─── Events ───────────────────────────────────────────────────────────────────

sealed class TtsEvent {
    data class SpeechStarted(val utteranceId: String) : TtsEvent()
    data class SpeechCompleted(val utteranceId: String) : TtsEvent()
    data class SpeechError(val utteranceId: String, val message: String) : TtsEvent()
}

sealed class SttEvent {
    object ReadyForSpeech : SttEvent()
    object SpeechBegun : SttEvent()
    object SpeechEnded : SttEvent()
    data class PartialResult(val text: String) : SttEvent()
    data class Result(val text: String, val alternatives: List<String>) : SttEvent()
    data class Error(val code: Int, val message: String) : SttEvent()
}
