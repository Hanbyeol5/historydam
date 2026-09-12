package com.samdori93.yeoksadam.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samdori93.yeoksadam.core.designsystem.theme.DancheongColors
import com.samdori93.yeoksadam.core.designsystem.theme.YeoksadamTheme
import com.samdori93.yeoksadam.core.ui.navigation.TopLevelDestination

/**
 * 홈 하단 5탭. 가운데(홈) 탭이 진사색 원형으로 돌출(목업 `.nav`).
 */
@Composable
fun YeoksadamBottomBar(
    current: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, DancheongColors.Hanji),
                ),
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom,
    ) {
        TopLevelDestination.entries.forEach { dest ->
            if (dest == TopLevelDestination.HOME) {
                HomeTab(selected = current == dest, onClick = { onSelect(dest) })
            } else {
                NavTab(dest = dest, selected = current == dest, onClick = { onSelect(dest) })
            }
        }
    }
}

@Composable
private fun NavTab(
    dest: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) DancheongColors.Jujak else DancheongColors.MeokSoft
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(dest.icon, contentDescription = dest.label, tint = tint, modifier = Modifier.size(22.dp))
        Text(dest.label, color = tint, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HomeTab(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(y = (-22).dp)
            .size(50.dp)
            .shadow(10.dp, RoundedCornerShape(17.dp), clip = false)
            .background(DancheongColors.Hanji, RoundedCornerShape(22.dp))
            .padding(2.dp)
            .background(DancheongColors.Jujak, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Home,
            contentDescription = "홈",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Preview(backgroundColor = 0xFFF4EAD9, showBackground = true)
@Composable
private fun YeoksadamBottomBarPreview() {
    YeoksadamTheme {
        YeoksadamBottomBar(current = TopLevelDestination.HOME, onSelect = {})
    }
}
