package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.HeritageVisionResult
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage

/**
 * 멀티모달 AI 비전으로 사진 속 문화재/유물을 판별하는 계약.
 * 구현(core:data)은 Gemini API 를 호출한다. (실서비스는 백엔드 `/v1/vision/recognize` 경유 권장)
 */
interface VisionRepository {
    /**
     * @param imageJpeg 촬영한 사진(JPEG 바이트)
     * @param candidates 현재 위치 주변 국가유산(판별 힌트로 함께 전달)
     */
    suspend fun recognize(imageJpeg: ByteArray, candidates: List<RecognizedHeritage>): Result<HeritageVisionResult>
}
