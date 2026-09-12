package com.samdori93.yeoksadam.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 인물 캐시 엔티티. relatedSiteIds 는 콤마 join 으로 저장(간이). */
@Entity(tableName = "figures")
data class FigureEntity(
    @PrimaryKey val id: String,
    val name: String,
    val title: String,
    val portraitUrl: String,
    val cutoutUrl: String?,
    val relatedSiteIds: String,
    val voiceId: String?,
)
