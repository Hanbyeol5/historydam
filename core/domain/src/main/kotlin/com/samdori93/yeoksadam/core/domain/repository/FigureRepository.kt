package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import kotlinx.coroutines.flow.Flow

/**
 * 인물·유적지 데이터 접근 계약.
 * 목록/스트림은 Flow, 단발 작업은 Result 로 반환한다(CLAUDE.md §4).
 */
interface FigureRepository {
    /** 현재 위치 기준 주변 인물 스트림. */
    fun observeNearbyFigures(origin: LatLng, radiusM: Int): Flow<List<NearbyFigure>>

    /** 전체 인물 목록(가나다 정렬은 UseCase/표현계층에서). */
    fun observeFigures(): Flow<List<Figure>>

    /** 단일 인물 상세. */
    suspend fun getFigure(id: String): Result<Figure>
}
