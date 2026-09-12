package com.samdori93.yeoksadam.core.domain.model

/** 역사 인물 (CLAUDE.md §7). */
data class Figure(
    val id: String,
    val name: String,
    val title: String,
    val portraitUrl: String,
    val cutoutUrl: String? = null,
    val relatedSiteIds: List<String> = emptyList(),
    val voiceId: String? = null,
)

/** 유적지. */
data class HeritageSite(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val description: String,
    val geofenceRadiusM: Float,
)

/** 유물. */
data class Relic(
    val id: String,
    val name: String,
    val era: String,
    val relatedFigureIds: List<String>,
)

/** 홈/지도에서 노출하는 "내 주변 인물" 카드 데이터. */
data class NearbyFigure(
    val figure: Figure,
    val site: HeritageSite,
    val distanceM: Float,
    val bearingDeg: Float,
)

/** AR 탐색 대상. */
data class ArTarget(
    val figureId: String,
    val siteId: String,
    val lat: Double,
    val lng: Double,
    val altitude: Double? = null,
    val headingDeg: Float? = null,
    val foundThresholdM: Float = 5f,
    val bearingToleranceDeg: Float = 20f,
)
