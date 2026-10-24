package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.domain.model.LatLng
import kotlinx.coroutines.flow.Flow

/** 디바이스 현재 위치 스트림 제공(FusedLocation 구현은 core:data). */
interface LocationRepository {
    fun observeLocation(): Flow<LatLng>

    /**
     * 지금 위치 한 번. 권한이 있으면 실제 위치(마지막 위치 우선), 없거나 측위 실패면 기본 위치.
     * `observeLocation().first()` 는 항상 기본 위치가 먼저 나오므로 단발 조회에는 이것을 쓴다.
     */
    suspend fun currentLocation(): LatLng
}
