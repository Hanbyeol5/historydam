package com.samdori93.yeoksadam.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.samdori93.yeoksadam.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages WHERE figureId = :figureId ORDER BY timestamp ASC")
    fun getMessagesByFigureId(figureId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE figureId = :figureId")
    suspend fun clearHistory(figureId: String)
}