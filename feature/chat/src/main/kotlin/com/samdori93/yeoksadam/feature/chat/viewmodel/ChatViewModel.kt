package com.samdori93.yeoksadam.feature.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samdori93.yeoksadam.core.ui.sample.SampleData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatLine(val mine: Boolean, val text: String)

data class ChatUiState(
    val messages: List<ChatLine> = emptyList(),
    val input: String = "",
    val responding: Boolean = false,
)

/**
 * 챗봇 대화 상태. 현재는 로컬 페르소나 응답(canned)으로 동작하며,
 * 추후 POST /v1/chat/stream(SSE) 으로 교체한다(CLAUDE.md §8).
 */
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(messages = SampleData.chatTranscript.map { ChatLine(mine = !it.first, text = it.second) }),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun onSend() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.responding) return
        _uiState.update {
            it.copy(messages = it.messages + ChatLine(mine = true, text = text), input = "", responding = true)
        }
        viewModelScope.launch {
            delay(RESPONSE_DELAY_MS)
            _uiState.update {
                it.copy(messages = it.messages + ChatLine(mine = false, text = personaReply(text)), responding = false)
            }
        }
    }

    private fun personaReply(userText: String): String {
        // 간이 페르소나 응답 — 추후 RAG 응답으로 교체
        val keyword = REPLIES.keys.firstOrNull { userText.contains(it) }
        return REPLIES[keyword] ?: GENERIC.random()
    }

    private companion object {
        const val RESPONSE_DELAY_MS = 700L

        val REPLIES = mapOf(
            "화성" to "수원 화성은 정조 임금의 효심과 백성을 향한 뜻이 깃든 성곽일세. 거중기로 백성의 수고를 크게 덜었지.",
            "정조" to "정조 임금이야말로 학문과 애민을 겸비한 군주셨네. 나 또한 그분의 뜻을 받들었을 뿐이네.",
            "정약용" to "다산은 영민하기 그지없었지. 거중기를 고안해 큰 공사를 손쉽게 마쳤느니라.",
            "백성" to "정치의 근본은 백성을 편안케 하는 데 있네. 곤궁을 내 일처럼 여겨야 하느니라.",
        )
        val GENERIC = listOf(
            "허허, 흥미로운 물음이로구나. 좀 더 자세히 들려주겠는가?",
            "그 일이라면 내 경험을 빌려 말해줄 수 있겠네.",
            "역사를 돌이켜 보면 그 답이 보일 것이야.",
        )
    }
}
