package com.samdori93.yeoksadam.core.domain.repository

import com.samdori93.yeoksadam.core.domain.model.Discovery
import kotlinx.coroutines.flow.Flow

/** 역사의 전당 도감 — 발견 항목 저장/조회 (로컬 영속: DataStore). */
interface DiscoveryRepository {
    /** 발견 목록 스트림(최신순). */
    fun observeDiscoveries(): Flow<List<Discovery>>

    /** 발견 기록(같은 type+refId 는 최초 발견 시각 유지하며 갱신). */
    suspend fun record(discovery: Discovery)

    /** 발견 항목 삭제(type+refId 기준). */
    suspend fun delete(discovery: Discovery)
}
