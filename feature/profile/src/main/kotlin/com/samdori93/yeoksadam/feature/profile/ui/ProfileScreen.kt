package com.samdori93.yeoksadam.feature.profile.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samdori93.yeoksadam.core.designsystem.component.LockedMedallion
import com.samdori93.yeoksadam.core.designsystem.component.MedallionPortrait
import com.samdori93.yeoksadam.core.designsystem.component.MonoMedallion
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.NanumMyeongjo
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme

/** 내 프로필 · 역사의 전당 도감 (목업 8번). */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableIntStateOf(1) } // 0 유적지, 1 인물, 2 유물
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DancheongColors.Hanji)
            .statusBarsPadding(),
    ) {
        // 상단바
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Home, "홈", tint = DancheongColors.Meok, modifier = Modifier.size(24.dp).clickable(onClick = onBack))
            Text(
                "내 프로필",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontFamily = NanumMyeongjo,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DancheongColors.Meok,
            )
            Icon(Icons.Filled.MoreHoriz, "더보기", tint = DancheongColors.Meok, modifier = Modifier.size(24.dp))
        }

        // 내 카드
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(DancheongColors.HanjiCard, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoMedallion(text = "희", size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("희수", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DancheongColors.Meok)
                Text("역사 탐험가", fontSize = 12.sp, color = DancheongColors.MeokSoft)
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Star, null, tint = DancheongColors.Hwangto, modifier = Modifier.size(13.dp))
                    Text("만난 인물 1명", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DancheongColors.CheongnokDeep)
                }
            }
        }

        // 역사의 전당
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier.size(24.dp).background(DancheongColors.Jujak, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("堂", color = androidx.compose.ui.graphics.Color.White, fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text("역사의 전당", fontFamily = NanumMyeongjo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DancheongColors.Meok)
        }

        // 탭
        Row(
            modifier = Modifier
                .padding(16.dp)
                .background(DancheongColors.HanjiDim, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("유적지", "인물", "유물").forEachIndexed { i, label ->
                val on = i == tab
                Box(
                    modifier = Modifier
                        .background(if (on) DancheongColors.HanjiCard else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable { tab = i }
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                ) {
                    Text(label, color = if (on) DancheongColors.Jujak else DancheongColors.MeokSoft, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }

        // 도감 그리드
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(galleryItems) { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (item.discovered) {
                        MedallionPortrait(portraitUrl = null, name = item.name, size = 84.dp)
                    } else {
                        LockedMedallion(size = 84.dp, label = "?")
                    }
                    Spacer(Modifier.size(6.dp))
                    Text(
                        if (item.discovered) item.name else "???",
                        fontSize = 12.sp,
                        color = if (item.discovered) DancheongColors.Meok else DancheongColors.MeokSoft,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private data class GalleryItem(val name: String, val discovered: Boolean)

private val galleryItems = buildList {
    add(GalleryItem("채제공", true))
    repeat(8) { add(GalleryItem("???", false)) }
}

@Preview(heightDp = 800)
@Composable
private fun ProfilePreview() {
    YeoksadamTheme {
        ProfileScreen({})
    }
}
