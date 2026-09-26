package com.samdori93.yeoksadam.feature.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.Role
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
 * core:domain의 ChatRepository를 통해 AI 서버 및 RAG 응답을 수신합니다.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    // Navigation을 통해 들어온 현재 인물의 figureId 자동 추출
    private val chatArgs = savedStateHandle.toRoute<Chat>()
    val figureId: String = chatArgs.figureId

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun onSend() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.responding) return

        // 1. 사용자가 입력한 메시지를 화면 목록에 추가
        val userMessage = ChatMessage(
            role = Role.USER,
            text = text
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                responding = true
            )
        }

        // 2. 현재 방의 figureId를 이용해 Repository에 메시지 전송
        viewModelScope.launch {
            chatRepository.sendMessage(figureId, text).collect { aiResponse ->
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiResponse,
                        responding = false
                    )
                }
            }
        }
    }
}