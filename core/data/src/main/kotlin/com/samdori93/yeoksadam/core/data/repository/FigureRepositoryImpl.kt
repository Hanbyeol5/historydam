package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.common.error.AppError
import com.samdori93.yeoksadam.core.common.geo.GeoMath
import com.samdori93.yeoksadam.core.common.result.Result
import com.samdori93.yeoksadam.core.data.local.FakeFigureCatalog
import com.samdori93.yeoksadam.core.domain.model.Figure
import com.samdori93.yeoksadam.core.domain.model.LatLng
import com.samdori93.yeoksadam.core.domain.model.NearbyFigure
import com.samdori93.yeoksadam.core.domain.repository.FigureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * FigureRepository 구현.
 * 현재는 [FakeFigureCatalog] 시드로 동작하며, 거리/방위는 [GeoMath] 로 실시간 계산한다.
 * TODO: YeoksadamApi(/v1/nearby, /v1/figures) + Room 캐시로 교체 (CLAUDE.md §8).
 */
class FigureRepositoryImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : FigureRepository {

    override fun observeNearbyFigures(origin: LatLng, radiusM: Int): Flow<List<NearbyFigure>> = flow {
        val nearby = FakeFigureCatalog.figures.mapNotNull { figure ->
            val site = FakeFigureCatalog.primarySiteOf(figure) ?: return@mapNotNull null
            val distance = GeoMath.distanceMeters(origin.lat, origin.lng, site.lat, site.lng)
            val bearing = GeoMath.bearingDegrees(origin.lat, origin.lng, site.lat, site.lng)
            NearbyFigure(figure = figure, site = site, distanceM = distance, bearingDeg = bearing)
        }.filter { it.distanceM <= radiusM }
        emit(nearby)
    }.flowOn(dispatchers.io)

    override fun observeFigures(): Flow<List<Figure>> = flow {
        emit(FakeFigureCatalog.figures.sortedBy { it.name })
    }.flowOn(dispatchers.io)

    override suspend fun getFigure(id: String): Result<Figure> {
        val figure = FakeFigureCatalog.figures.firstOrNull { it.id == id }
        return if (figure != null) {
            Result.Success(figure)
        } else {
            Result.Failure(AppError.Server(code = 404, message = "인물을 찾을 수 없습니다: $id"))
        }
    }
}
