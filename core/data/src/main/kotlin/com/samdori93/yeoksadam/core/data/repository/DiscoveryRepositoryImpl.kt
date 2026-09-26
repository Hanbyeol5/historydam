package com.samdori93.yeoksadam.core.data.repository

import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.datastore.DiscoveryStore
import com.samdori93.yeoksadam.core.domain.model.Discovery
import com.samdori93.yeoksadam.core.domain.model.DiscoveryType
import com.samdori93.yeoksadam.core.domain.repository.DiscoveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * [DiscoveryStore](DataStore) 에 도감을 JSON 으로 영속화하는 [DiscoveryRepository] 구현.
 * 같은 (type, refId) 재기록 시 최초 발견 시각은 유지하고 나머지 정보만 갱신한다.
 */
class DiscoveryRepositoryImpl @Inject constructor(
    private val store: DiscoveryStore,
    private val dispatchers: DispatcherProvider,
) : DiscoveryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeDiscoveries(): Flow<List<Discovery>> =
        store.discoveriesJson.map { raw -> decode(raw).sortedByDescending { it.discoveredAt } }

    override suspend fun record(discovery: Discovery) = withContext(dispatchers.io) {
        val current = decode(store.discoveriesJson.first())
        val existing = current.firstOrNull { it.type == discovery.type && it.refId == discovery.refId }
        val merged = if (existing != null) {
            current.map {
                if (it.type == discovery.type && it.refId == discovery.refId) {
                    discovery.copy(discoveredAt = existing.discoveredAt)
                } else {
                    it
                }
            }
        } else {
            current + discovery
        }
        store.setDiscoveriesJson(encode(merged))
    }

    override suspend fun delete(discovery: Discovery) = withContext(dispatchers.io) {
        val remaining = decode(store.discoveriesJson.first())
            .filterNot { it.type == discovery.type && it.refId == discovery.refId }
        store.setDiscoveriesJson(encode(remaining))
    }

    private fun decode(raw: String): List<Discovery> =
        if (raw.isBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString<List<DiscoveryEntity>>(raw).map { it.toDomain() }
            }.getOrDefault(emptyList())
        }

    private fun encode(list: List<Discovery>): String =
        json.encodeToString(list.map { DiscoveryEntity.from(it) })
}

@Serializable
private data class DiscoveryEntity(
    val type: String,
    val refId: String,
    val name: String,
    val subtitle: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val discoveredAt: Long = 0L,
) {
    fun toDomain() = Discovery(
        type = runCatching { DiscoveryType.valueOf(type) }.getOrDefault(DiscoveryType.RELIC),
        refId = refId,
        name = name,
        subtitle = subtitle,
        description = description,
        imageUrl = imageUrl,
        discoveredAt = discoveredAt,
    )

    companion object {
        fun from(d: Discovery) = DiscoveryEntity(
            type = d.type.name,
            refId = d.refId,
            name = d.name,
            subtitle = d.subtitle,
            description = d.description,
            imageUrl = d.imageUrl,
            discoveredAt = d.discoveredAt,
        )
    }
}
