package com.samdori93.yeoksadam.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.samdori93.yeoksadam.core.database.entity.FigureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FigureDao {

    @Query("SELECT * FROM figures ORDER BY name ASC")
    fun observeAll(): Flow<List<FigureEntity>>

    @Query("SELECT * FROM figures WHERE id = :id")
    suspend fun getById(id: String): FigureEntity?

    @Upsert(entity = FigureEntity::class)
    suspend fun upsertAll(figures: List<FigureEntity>)

    @Query("DELETE FROM figures")
    suspend fun clear()
}
