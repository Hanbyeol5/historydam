package com.samdori93.yeoksadam.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 로그인 세션 (서버가 준 토큰 · 아이디 · 이름) — 비밀번호는 저장하지 않는다. */
@Singleton
class AuthStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    data class Session(val token: String, val id: String, val name: String)

    val session: Flow<Session?> = dataStore.data.map { p ->
        val token = p[TOKEN] ?: return@map null
        Session(token, p[ID].orEmpty(), p[NAME].orEmpty())
    }

    suspend fun save(session: Session) {
        dataStore.edit {
            it[TOKEN] = session.token
            it[ID] = session.id
            it[NAME] = session.name
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(TOKEN)
            it.remove(ID)
            it.remove(NAME)
        }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("auth_token")
        val ID = stringPreferencesKey("auth_id")
        val NAME = stringPreferencesKey("auth_name")
    }
}
