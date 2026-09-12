package com.samdori93.yeoksadam.core.network.api

import com.samdori93.yeoksadam.core.network.dto.FigureDto
import com.samdori93.yeoksadam.core.network.dto.NearbyResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** 백엔드 계약 (CLAUDE.md §8). */
interface YeoksadamApi {

    @GET("v1/nearby")
    suspend fun getNearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radiusM: Int,
    ): NearbyResponseDto

    @GET("v1/figures")
    suspend fun getFigures(): List<FigureDto>

    @GET("v1/figures/{id}")
    suspend fun getFigure(@Path("id") id: String): FigureDto
}
