package com.samdori93.yeoksadam.core.domain.usecase

import com.samdori93.yeoksadam.core.domain.model.Discovery
import com.samdori93.yeoksadam.core.domain.repository.DiscoveryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 역사의 전당 도감 목록 구독. */
class ObserveDiscoveriesUseCase @Inject constructor(
    private val repository: DiscoveryRepository,
) {
    operator fun invoke(): Flow<List<Discovery>> = repository.observeDiscoveries()
}

/** 발견 항목 기록(유물/유적지/인물). */
class RecordDiscoveryUseCase @Inject constructor(
    private val repository: DiscoveryRepository,
) {
    suspend operator fun invoke(discovery: Discovery) = repository.record(discovery)
}

/** 발견 항목 삭제. */
class RemoveDiscoveryUseCase @Inject constructor(
    private val repository: DiscoveryRepository,
) {
    suspend operator fun invoke(discovery: Discovery) = repository.delete(discovery)
}
