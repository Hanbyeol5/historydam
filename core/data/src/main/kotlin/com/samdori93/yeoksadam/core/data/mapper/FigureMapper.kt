package com.samdori93.yeoksadam.core.data.mapper

import com.samdori93.yeoksadam.core.database.entity.FigureEntity
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.HeritageSite
import com.samdori93.yeoksadam.core.network.dto.FigureDto
import com.samdori93.yeoksadam.core.network.dto.HeritageSiteDto

/** DTO/Entity ↔ 도메인 모델 매핑. raw 타입을 표현계층까지 노출하지 않는다(CLAUDE.md §4). */

fun FigureDto.toDomain(): Figure = Figure(
    id = id,
    name = name,
    title = title,
    portraitUrl = portraitUrl,
    cutoutUrl = cutoutUrl,
    relatedSiteIds = relatedSiteIds,
    voiceId = voiceId,
)

fun HeritageSiteDto.toDomain(): HeritageSite = HeritageSite(
    id = id,
    name = name,
    lat = lat,
    lng = lng,
    description = description,
    geofenceRadiusM = geofenceRadiusM,
)

fun Figure.toEntity(): FigureEntity = FigureEntity(
    id = id,
    name = name,
    title = title,
    portraitUrl = portraitUrl,
    cutoutUrl = cutoutUrl,
    relatedSiteIds = relatedSiteIds.joinToString(","),
    voiceId = voiceId,
)

fun FigureEntity.toDomain(): Figure = Figure(
    id = id,
    name = name,
    title = title,
    portraitUrl = portraitUrl,
    cutoutUrl = cutoutUrl,
    relatedSiteIds = relatedSiteIds.split(",").filter { it.isNotBlank() },
    voiceId = voiceId,
)
