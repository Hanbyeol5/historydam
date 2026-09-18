package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.common.error.AppError
import com.samdori93.yeoksadam.core.common.geo.GeoMath
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.RecognizedHeritage
import com.samdori93.yeoksadam.core.domain.repository.HeritageRepository
import com.samdori93.yeoksadam.core.network.heritage.HeritageDetailDto
import com.samdori93.yeoksadam.core.network.heritage.HeritageItemDto
import com.samdori93.yeoksadam.core.network.heritage.HeritageRemoteDataSource
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * 국가유산청 국가유산정보 OpenAPI 기반 [HeritageRepository] 구현.
 *
 * 후보 목록(국보·사적)은 자주 바뀌지 않으므로 최초 1회 받아 메모리에 캐시하고,
 * 현재 위치와의 거리는 [GeoMath] 로 실시간 계산해 가까운 순으로 반환한다.
 */
class HeritageRepositoryImpl @Inject constructor(
    private val remote: HeritageRemoteDataSource,
    private val dispatchers: DispatcherProvider,
) : HeritageRepository {

    @Volatile
    private var candidateCache: List<HeritageItemDto>? = null

    override suspend fun getNearbyHeritage(origin: LatLng, limit: Int): Result<List<RecognizedHeritage>> =
        withContext(dispatchers.io) {
            runCatching {
                val items = candidateCache ?: loadCandidates().also { candidateCache = it }
                items.asSequence()
                    .mapNotNull { dto ->
                        val lat = dto.lat
                        val lng = dto.lng
                        if (lat == null || lng == null) {
                            null
                        } else {
                            dto to GeoMath.distanceMeters(origin.lat, origin.lng, lat, lng)
                        }
                    }
                    .sortedBy { it.second }
                    .take(limit)
                    .map { (dto, distance) -> dto.toRecognized(distance) }
                    .toList()
            }.toAppResult()
        }

    override suspend fun getHeritageDetail(id: String): Result<RecognizedHeritage> =
        withContext(dispatchers.io) {
            runCatching {
                val parts = id.split("_")
                require(parts.size == 3) { "잘못된 국가유산 식별자: $id" }
                val detail = remote.fetchDetail(kdcd = parts[0], asno = parts[1], ctcd = parts[2])
                    ?: error("상세 정보를 찾을 수 없습니다.")
                detail.toRecognized(id)
            }.toAppResult()
        }

    private fun loadCandidates(): List<HeritageItemDto> =
        CANDIDATE_KINDS.flatMap { remote.fetchList(kdcd = it) }

    private fun HeritageItemDto.toRecognized(distanceM: Float) = RecognizedHeritage(
        id = "${kdcd}_${asno}_$ctcd",
        name = nameKo,
        hanja = nameHanja,
        kind = kind,
        era = "",
        category = "",
        address = "$ctcdName $siName".trim(),
        description = "",
        imageUrl = null,
        lat = lat ?: 0.0,
        lng = lng ?: 0.0,
        distanceM = distanceM,
    )

    private fun HeritageDetailDto.toRecognized(id: String) = RecognizedHeritage(
        id = id,
        name = nameKo,
        hanja = nameHanja,
        kind = kind,
        era = era,
        category = category,
        address = address,
        description = content,
        imageUrl = imageUrl,
        lat = lat ?: 0.0,
        lng = lng ?: 0.0,
        distanceM = 0f,
    )

    private fun <T> kotlin.Result<T>.toAppResult(): Result<T> = fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            val error = if (e is IOException) AppError.Network(e) else AppError.Unknown(e)
            Result.Failure(error)
        },
    )

    private companion object {
        /** 건물·유적 중심 후보: 국보(11) + 사적(13). */
        val CANDIDATE_KINDS = listOf("11", "13")
    }
}
