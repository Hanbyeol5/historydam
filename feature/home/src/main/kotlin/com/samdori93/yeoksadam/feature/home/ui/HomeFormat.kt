package com.samdori93.yeoksadam.feature.home.ui

import kotlin.math.roundToInt

/** 거리(m) 를 사람이 읽기 좋은 문자열로. */
internal fun formatDistance(meters: Float): String = when {
    meters < 1_000 -> "${meters.roundToInt()}m"
    else -> "%.1fkm".format(meters / 1_000)
}
