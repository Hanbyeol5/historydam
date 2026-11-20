package com.samdori93.yeoksadam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.domain.repository.ServerStoreRepository
import com.samdori93.yeoksadam.core.ui.ar.FigureModelSource
import com.samdori93.yeoksadam.core.ui.ar.LocalFigureModelSource
import com.samdori93.yeoksadam.navigation.YeoksadamAppRoot
import androidx.lifecycle.lifecycleScope
import com.samdori93.yeoksadam.feature.notification.geofence.GeofenceSync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var geofenceSync: GeofenceSync
    @Inject lateinit var serverStore: ServerStoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 위치 권한이 있으면 유적 도착 지오펜스를 다시 맞춘다 (재부팅·업데이트로 지워진 것 복구, 주변 유적 갱신)
        lifecycleScope.launch { runCatching { geofenceSync.sync() } }
        setContent {
            // 서버에 올린 3D 인물 모델 — AR·대화 화면이 인물을 만나면 받아 온다 (core:ui rememberFigure3dModel)
            val modelSource = FigureModelSource(serverStore.models) { id -> runCatching { serverStore.syncModel(id) } }
            CompositionLocalProvider(LocalFigureModelSource provides modelSource) {
                YeoksadamTheme {
                    YeoksadamAppRoot(onLocationGranted = { lifecycleScope.launch { runCatching { geofenceSync.sync() } } })
                }
            }
        }
    }
}
