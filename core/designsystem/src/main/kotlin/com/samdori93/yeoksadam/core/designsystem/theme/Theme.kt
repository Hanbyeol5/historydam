package com.samdori93.yeoksadam.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = DancheongColors.Jujak,
    onPrimary = DancheongColors.Baek,
    primaryContainer = DancheongColors.JujakLight,
    onPrimaryContainer = DancheongColors.JujakDeep,
    secondary = DancheongColors.Cheongnok,
    onSecondary = DancheongColors.Baek,
    secondaryContainer = DancheongColors.CheongnokLight,
    onSecondaryContainer = DancheongColors.CheongnokDeep,
    tertiary = DancheongColors.Hwangto,
    onTertiary = DancheongColors.Meok,
    background = DancheongColors.Hanji,
    onBackground = DancheongColors.Meok,
    surface = DancheongColors.Baek,
    onSurface = DancheongColors.Meok,
    surfaceVariant = DancheongColors.HanjiDim,
    onSurfaceVariant = DancheongColors.MeokSoft,
    outline = DancheongColors.MeokSoft,
)

private val DarkColors = darkColorScheme(
    primary = DancheongColors.JujakLight,
    onPrimary = DancheongColors.Meok,
    primaryContainer = DancheongColors.JujakDeep,
    onPrimaryContainer = DancheongColors.Baek,
    secondary = DancheongColors.CheongnokLight,
    onSecondary = DancheongColors.Meok,
    tertiary = DancheongColors.HwangtoLight,
    onTertiary = DancheongColors.Meok,
    background = DancheongColors.HanjiDark,
    onBackground = DancheongColors.Hanji,
    surface = DancheongColors.HanjiDark,
    onSurface = DancheongColors.Hanji,
    surfaceVariant = DancheongColors.MeokSoft,
    onSurfaceVariant = DancheongColors.HanjiDim,
    outline = DancheongColors.HanjiDim,
)

/**
 * 앱 전역 테마. 단청 색채 + 한지 배경 + 명조/고딕 타이포.
 * 모든 @Preview 는 이 테마로 래핑한다(CLAUDE.md §5).
 */
@Composable
fun YeoksadamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YeoksadamTypography,
        shapes = YeoksadamShapes,
        content = content,
    )
}
