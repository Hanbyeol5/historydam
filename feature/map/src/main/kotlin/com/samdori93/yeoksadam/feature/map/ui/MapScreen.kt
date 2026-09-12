package com.samdori93.yeoksadam.feature.map.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.compose.CameraPositionState
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.compose.rememberFusedLocationSource
import com.naver.maps.map.compose.rememberMarkerState
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.ui.sample.SampleData

/** 지도 (목업 4번) — 네이버 지도 + 유적지별 인물 핑 · AR 길찾기. */
@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun MapScreen(
    onEnterAr: (figureId: String, siteId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pins = SampleData.mapPins
    var selected by remember { mutableStateOf(pins.first()) }

    // 서울 유적지 군집 중심
    val cameraPositionState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition(LatLng(37.5750, 126.9830), 13.0)
    }

    // 위치 권한
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> hasLocationPermission = result.values.any { it } }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }
    val locationSource = rememberFusedLocationSource()

    Box(modifier = modifier.fillMaxSize().background(DancheongColors.HanjiDim)) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            locationSource = locationSource,
            properties = MapProperties(
                locationTrackingMode = if (hasLocationPermission) LocationTrackingMode.NoFollow else LocationTrackingMode.None,
            ),
            uiSettings = MapUiSettings(
                isZoomControlEnabled = false,
                isCompassEnabled = true,
                isScaleBarEnabled = false,
                isLocationButtonEnabled = true,
            ),
        ) {
            pins.forEach { pin ->
                Marker(
                    state = rememberMarkerState(key = pin.siteId, position = LatLng(pin.lat, pin.lng)),
                    captionText = "${pin.figureName} · ${pin.siteName}",
                    onClick = {
                        selected = pin
                        true
                    },
                )
            }
        }

        // 경로 헤더 (선택된 유적지 반영)
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .background(DancheongColors.HanjiCard, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(34.dp).background(DancheongColors.Cheongnok, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DirectionsWalk, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${selected.siteName}까지 걷기", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DancheongColors.Meok)
                Text("${selected.figureName} 님이 기다리는 곳 · ${selected.distanceLabel}", fontSize = 11.sp, color = DancheongColors.MeokSoft)
            }
            Box(
                modifier = Modifier.background(DancheongColors.Jujak, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text("AR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // 하단 카드 (선택된 인물)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .background(DancheongColors.HanjiCard, RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedallionPortrait(portraitUrl = null, name = selected.figureName, sealMark = selected.figureName.take(1), size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(selected.figureName, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Meok)
                Text("${selected.siteName} · ${selected.distanceLabel}", fontSize = 12.sp, color = DancheongColors.MeokSoft)
            }
            Row(
                modifier = Modifier
                    .background(DancheongColors.Jujak, RoundedCornerShape(20.dp))
                    .clickable { onEnterAr(selected.figureId, selected.siteId) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Text("찾기", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Preview(heightDp = 800)
@Composable
private fun MapPreview() {
    YeoksadamTheme {
        Box(Modifier.fillMaxSize().background(DancheongColors.HanjiDim))
    }
}
