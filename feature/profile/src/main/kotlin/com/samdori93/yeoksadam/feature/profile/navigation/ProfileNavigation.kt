package com.samdori93.yeoksadam.feature.profile.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.samdori93.yeoksadam.feature.profile.ui.ProfileScreen
import kotlinx.serialization.Serializable

/** 내 프로필 · 역사의 전당(도감) 라우트 (CLAUDE.md §6 #8). */
@Serializable
data object Profile

fun NavController.navigateToProfile(navOptions: NavOptions? = null) =
    navigate(Profile, navOptions)

fun NavGraphBuilder.profileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable<Profile> {
        ProfileScreen(onBack = onBack, modifier = modifier)
    }
}
