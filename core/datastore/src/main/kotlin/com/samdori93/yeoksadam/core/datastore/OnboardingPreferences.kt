package com.samdori93.yeoksadam.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 온보딩/세션/권한 안내 상태 등 경량 환경설정 (CLAUDE.md §2 DataStore). */
@Singleton
class OnboardingPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[KEY_ONBOARDING] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING] = completed }
    }

    private companion object {
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
    }
}
