package com.samdori93.yeoksadam.feature.voice.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject

enum class VoiceStatus { IDLE, LISTENING, THINKING, SPEAKING }

data class VoiceLine(val mine: Boolean, val text: String)

data class VoiceUiState(
    val status: VoiceStatus = VoiceStatus.IDLE,
    val transcript: List<VoiceLine> = emptyList(),
    val partial: String = "",
    val ttsReady: Boolean = false,
)

/**
 * 초상화 음성 대화 — 온디바이스 STT(SpeechRecognizer) + TTS(TextToSpeech).
 * 서버 인물 보이스 TTS 연동 전, Android 기본 음성으로 동작(CLAUDE.md §11 폴백).
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
                _uiState.update { it.copy(ttsReady = true) }
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _uiState.update { it.copy(status = VoiceStatus.SPEAKING) }
            }
            override fun onDone(utteranceId: String?) {
                _uiState.update { it.copy(status = VoiceStatus.IDLE) }
            }
            @Deprecated("deprecated")
            override fun onError(utteranceId: String?) {
                _uiState.update { it.copy(status = VoiceStatus.IDLE) }
            }
        })
    }

    /** 마이크 버튼: 듣는 중이면 중지, 아니면 듣기 시작. 말하는 중이면 무시. */
    fun onMic() {
        when (_uiState.value.status) {
            VoiceStatus.LISTENING -> stopListening()
            VoiceStatus.SPEAKING -> Unit
            else -> startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        _uiState.update { it.copy(status = VoiceStatus.LISTENING, partial = "") }
        recognizer?.startListening(intent)
    }

    private fun stopListening() {
        recognizer?.stopListening()
        _uiState.update { it.copy(status = VoiceStatus.THINKING) }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            _uiState.update { if (it.status == VoiceStatus.LISTENING) it.copy(status = VoiceStatus.THINKING) else it }
        }
        override fun onError(error: Int) {
            _uiState.update { it.copy(status = VoiceStatus.IDLE, partial = "") }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (text.isNotBlank()) _uiState.update { it.copy(partial = text) }
        }
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (text.isBlank()) {
                _uiState.update { it.copy(status = VoiceStatus.IDLE, partial = "") }
                return
            }
            _uiState.update {
                it.copy(transcript = it.transcript + VoiceLine(mine = true, text = text), partial = "")
            }
            respond(text)
        }
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun respond(userText: String) {
        val reply = personaReply(userText)
        _uiState.update {
            it.copy(transcript = it.transcript + VoiceLine(mine = false, text = reply), status = VoiceStatus.THINKING)
        }
        tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    private fun personaReply(userText: String): String {
        val keyword = REPLIES.keys.firstOrNull { userText.contains(it) }
        return REPLIES[keyword] ?: GENERIC.random()
    }

    override fun onCleared() {
        recognizer?.destroy()
        tts?.shutdown()
    }

    private companion object {
        val REPLIES = mapOf(
            "화성" to "수원 화성은 정조 임금의 효심과 백성을 향한 뜻이 깃든 성곽일세.",
            "백성" to "정치의 근본은 백성을 편안케 하는 데 있느니라.",
            "정조" to "정조 임금이야말로 학문과 애민을 겸비한 군주셨네.",
            "안녕" to "어서 오시게. 무엇이 궁금하신가?",
        )
        val GENERIC = listOf(
            "허허, 좋은 물음이로구나.",
            "내 경험으로 답하자면, 그 또한 백성을 위한 길이었네.",
            "역사를 돌이켜 보면 그 답이 보일 것이야.",
        )
    }
}
