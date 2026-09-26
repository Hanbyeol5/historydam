package com.samdori93.yeoksadam.core.domain.usecase

import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage
import com.samdori93.yeoksadam.core.domain.repository.HeritageRepository
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * 카메라 셔터 시점의 현재 위치를 잡아 주변 국가유산 후보를 인식한다.
 * 실제 좌표 측위를 잠시 기다리되(타임아웃), 실패 시 폴백 위치(경복궁)로 동작한다.
 */
class RecognizeNearbyHeritageUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val heritageRepository: HeritageRepository,
) {
    suspend operator fun invoke(limit: Int = HeritageRepository.DEFAULT_LIMIT): Result<List<RecognizedHeritage>> =
        heritageRepository.getNearbyHeritage(currentLocation(), limit)

    /**
     * observeLocation() 은 시작 시 폴백 위치(경복궁)를 먼저 emit 하므로,
     * 실측 위치(2번째 emit)를 짧게 기다렸다가 없으면 폴백을 사용한다.
     */
    private suspend fun currentLocation(): LatLng =
        withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            locationRepository.observeLocation().drop(1).first()
        } ?: locationRepository.observeLocation().first()

    private companion object {
        const val LOCATION_TIMEOUT_MS = 2_500L
    }
}
