package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.Citation
import com.samdori93.yeoksadam.core.domain.model.Role
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.core.network.api.YeoksadamApi
import com.samdori93.yeoksadam.core.network.dto.ChatRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: YeoksadamApi
) : ChatRepository {

    override fun sendMessage(figureId: String, message: String): Flow<ChatMessage> = flow {
        try {
            // 1. 실제 백엔드 AI RAG 서버 통신 시도
            val response = api.sendChatMessage(
                ChatRequestDto(
                    figureId = figureId,
                    message = message
                )
            )

            // 2. RAG 검색 결과(답변 + RAG 사료 근거 리스트) 변환
            val domainMessage = ChatMessage(
                role = Role.FIGURE,
                text = response.text,
                citations = response.citations.map { dto ->
                    Citation(
                        source = dto.source,
                        excerpt = dto.excerpt
                    )
                }
            )
            emit(domainMessage)
        } catch (e: Exception) {
            // 3. 백엔드 미연결 또는 네트워크 통신 실패 시 공통 에러 메시지 반환
            val errorMessage = ChatMessage(
                role = Role.FIGURE,
                text = "현재 AI 백엔드 서버와 연결할 수 없습니다. 네트워크 상태나 서버 주소를 확인해 주세요."
            )
            emit(errorMessage)
        }
    }

    override fun getChatHistory(figureId: String): Flow<List<ChatMessage>> = flow {
        emit(emptyList())
    }
}