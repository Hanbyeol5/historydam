package com.samdori93.yeoksadam.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /v1/nearby 응답 (CLAUDE.md §8). */
@Serializable
data class NearbyResponseDto(
    @SerialName("figures") val figures: List<NearbyFigureDto> = emptyList(),
)

@Serializable
data class NearbyFigureDto(
    @SerialName("figure") val figure: FigureDto,
    @SerialName("site") val site: HeritageSiteDto,
    @SerialName("distanceM") val distanceM: Float,
    @SerialName("bearingDeg") val bearingDeg: Float,
)

@Serializable
data class FigureDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("title") val title: String = "",
    @SerialName("portraitUrl") val portraitUrl: String = "",
    @SerialName("cutoutUrl") val cutoutUrl: String? = null,
    @SerialName("relatedSiteIds") val relatedSiteIds: List<String> = emptyList(),
    @SerialName("voiceId") val voiceId: String? = null,
)

@Serializable
data class HeritageSiteDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("description") val description: String = "",
    @SerialName("geofenceRadiusM") val geofenceRadiusM: Float = 100f,
)
