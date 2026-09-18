package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage

/**
 * 국가유산청 국가유산정보 OpenAPI 기반 문화재 조회 계약.
 *
 * CLAUDE.md §8 의 `/v1/vision/recognize` 를 대체하는 온디바이스 경로:
 * 서버 비전 모델이 준비되기 전까지 **위치(LBS)로 후보를 좁혀** 인식처럼 동작한다.
 */
interface HeritageRepository {
    /** 현재 위치에서 가까운 국가유산 후보를 거리순으로. (목록 단계 — 설명 없음) */
    suspend fun getNearbyHeritage(origin: LatLng, limit: Int = DEFAULT_LIMIT): Result<List<RecognizedHeritage>>

    /** 단일 국가유산 상세(설명·시대·이미지 포함). */
    suspend fun getHeritageDetail(id: String): Result<RecognizedHeritage>

    companion object {
        const val DEFAULT_LIMIT = 6
    }
}
