package com.samdori93.yeoksadam.feature.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.feature.chat.navigation.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val responding: Boolean = false,
)

/**
 * 챗봇 대화 상태.
 * core:domain의 ChatRepository를 통해 AI 서버 및 RAG 응답을 수신하고, 로컬 DB와 실시간 동기화합니다.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    // Navigation을 통해 들어온 현재 인물의 figureId 자동 추출 (예: "fig_yisunsin")
    private val chatArgs = savedStateHandle.toRoute<Chat>()
    val figureId: String = chatArgs.figureId

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // 1. 방에 진입 시 로컬 DB의 대화 기록을 실시간 구독(Subscribe)
        viewModelScope.launch {
            chatRepository.getChatHistory(figureId).collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun onSend() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.responding) return

        // 입력창 비우기 및 응답 대기 상태(로딩) 시작
        _uiState.update {
            it.copy(
                input = "",
                responding = true
            )
        }

        // 2. 메시지 전송 (Repository 내부에서 DB 저장 및 AI 백엔드 통신 처리)
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(figureId, text).collect {
                    // DB 저장이 완료되면 상단 init의 getChatHistory Flow가
                    // 자동으로 메시지 목록을 갱신해 주므로 별도 처리 없음
                }
            } finally {
                // 통신 완료 또는 실패 시 응답 대기 상태 해제
                _uiState.update { it.copy(responding = false) }
            }
        }
    }
}