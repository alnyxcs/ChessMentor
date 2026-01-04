package com.example.chessmentor.presentation.ui.components.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.chessmentor.presentation.ui.theme.BackgroundDark
import com.example.chessmentor.presentation.ui.theme.BackgroundLight
import com.example.chessmentor.presentation.ui.theme.ErrorRed
import com.example.chessmentor.presentation.ui.theme.PrimaryGreen
import com.example.chessmentor.presentation.ui.theme.PrimaryGreenDark
import com.example.chessmentor.presentation.ui.theme.SurfaceDark
import com.example.chessmentor.presentation.ui.theme.SurfaceLight
import com.example.chessmentor.presentation.ui.theme.TextPrimaryDark
import com.example.chessmentor.presentation.ui.theme.TextPrimaryLight

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryGreenDark,
    onPrimaryContainer = Color.White,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,

    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryGreen.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryGreenDark,

    background = BackgroundLight,
    onBackground = TextPrimaryLight,

    surface = SurfaceLight,
    onSurface = TextPrimaryLight,

    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ChessMentorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
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
        content = content
    )
}