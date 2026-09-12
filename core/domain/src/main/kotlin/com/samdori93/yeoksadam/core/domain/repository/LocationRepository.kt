package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.domain.model.LatLng
import kotlinx.coroutines.flow.Flow

/** 디바이스 현재 위치 스트림 제공(FusedLocation 구현은 core:data). */
interface LocationRepository {
    fun observeLocation(): Flow<LatLng>
}
