package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.database.dao.ChatDao
import com.samdori93.yeoksadam.core.database.entity.ChatMessageEntity
import com.samdori93.yeoksadam.core.domain.model.ChatMessage
import com.samdori93.yeoksadam.core.domain.model.Citation
import com.samdori93.yeoksadam.core.domain.model.Role
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.core.network.api.YeoksadamApi
import com.samdori93.yeoksadam.core.network.dto.ChatRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: YeoksadamApi,
    private val chatDao: ChatDao // 1. ChatDao 주입받기
) : ChatRepository {

    override fun sendMessage(figureId: String, message: String): Flow<ChatMessage> = flow {
        // 2. 사용자가 보낸 질문을 먼저 DB 서랍에 저장
        chatDao.insertMessage(
            ChatMessageEntity(
                figureId = figureId,
                role = Role.USER.name,
                text = message
            )
        )

        try {
            // 3. 백엔드 AI RAG 서버 통신 시도
            val response = api.sendChatMessage(
                ChatRequestDto(
                    figureId = figureId,
                    message = message
                )
            )

            // 4. 백엔드에서 받은 AI 답변을 DB 서랍에 저장
            chatDao.insertMessage(
                ChatMessageEntity(
                    figureId = figureId,
                    role = Role.FIGURE.name,
                    text = response.text
                )
            )

            // 5. 화면 표시용 Domain 모델로 변환하여 전달
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
            // 6. 예외 발생 시 안내 메시지 반환
            val errorMessage = ChatMessage(
                role = Role.FIGURE,
                text = "현재 AI 백엔드 서버와 연결할 수 없습니다. 네트워크 상태나 서버 주소를 확인해 주세요."
            )
            emit(errorMessage)
        }
    }

    override fun getChatHistory(figureId: String): Flow<List<ChatMessage>> {
        // 7. DB에서 저장된 대화 기록을 불러와 화면용 Domain 모델 리스트로 변환
        return chatDao.getMessagesByFigureId(figureId).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    role = Role.valueOf(entity.role),
                    text = entity.text
                )
            }
        }
    }
}