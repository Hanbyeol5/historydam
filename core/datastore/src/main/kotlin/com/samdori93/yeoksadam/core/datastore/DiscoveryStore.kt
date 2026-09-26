package com.samdori93.yeoksadam.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 역사의 전당 도감 원본(JSON 문자열) 영속 저장.
 * 직렬화/역직렬화·도메인 매핑은 core:data 의 DiscoveryRepositoryImpl 이 담당한다.
 */
@Singleton
class DiscoveryStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val discoveriesJson: Flow<String> = dataStore.data.map { it[KEY] ?: "" }

    suspend fun setDiscoveriesJson(json: String) {
        dataStore.edit { it[KEY] = json }
    }

    private companion object {
        val KEY = stringPreferencesKey("discoveries_json")
    }
}
