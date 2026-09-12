package com.samdori93.yeoksadam.core.common.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 위경도 거리/방위 계산 유틸 — 홈 거리표시, AR bearing 등에 공통 사용. */
object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** 두 좌표 사이 거리(m), Haversine. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_M * c).toFloat()
    }

    /** from → to 방위각(0~360, 북쪽 기준 시계방향). */
    fun bearingDegrees(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLng = Math.toRadians(lng2 - lng1)
        val y = sin(dLng) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLng)
        val deg = Math.toDegrees(atan2(y, x))
        return ((deg + 360) % 360).toFloat()
    }
}
