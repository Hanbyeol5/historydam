package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * 특정 인물에게 메시지를 보내고 RAG 근거가 포함된 AI 답변 스트림을 받습니다.
     */
    fun sendMessage(figureId: String, message: String): Flow<ChatMessage>

    /**
     * 특정 인물과의 기존 대화 내역을 불러옵니다.
     */
    fun getChatHistory(figureId: String): Flow<List<ChatMessage>>
}