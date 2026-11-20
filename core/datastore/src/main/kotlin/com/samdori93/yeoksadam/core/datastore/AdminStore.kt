package com.samdori93.yeoksadam.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 관리자 모드: 서버 업로드 키 · 3D 모델/RAG 자료 폴더 (서버 저장소 업로드용, 폰에만 저장). */
@Singleton
class AdminStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val uploadKey: Flow<String> = dataStore.data.map { it[UPLOAD_KEY] ?: "" }

    suspend fun setUploadKey(key: String) {
        dataStore.edit { it[UPLOAD_KEY] = key.trim() }
    }

    /** @param kind "MODEL" · "DOC" */
    fun folder(kind: String): Flow<String?> = dataStore.data.map { it[folderKey(kind)] }

    suspend fun setFolder(kind: String, treeUri: String) {
        dataStore.edit { it[folderKey(kind)] = treeUri }
    }

    private fun folderKey(kind: String) = stringPreferencesKey("store_folder_$kind")

    private companion object {
        val UPLOAD_KEY = stringPreferencesKey("store_upload_key")
    }
}
