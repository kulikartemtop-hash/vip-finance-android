package com.example.vipfinance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun VIPFinanceTheme(
    theme: String = "system",
    style: String = "classic",
    content: @Composable () -> Unit
) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = when (style) {
        "ocean" -> if (dark) darkColorScheme(
            primary = Color(0xFF73C7D8), secondary = Color(0xFF8CC9D5), tertiary = Color(0xFFB5C8FF),
            background = Color(0xFF081417), surface = Color(0xFF0E1E22)
        ) else lightColorScheme(
            primary = Color(0xFF006A78), secondary = Color(0xFF2E6670), tertiary = Color(0xFF4C5E91),
            background = Color(0xFFF4FBFC), surface = Color(0xFFFFFFFF)
        )
        "graphite" -> if (dark) darkColorScheme(
            primary = Color(0xFFD0BCFF), secondary = Color(0xFFBDB5CC), tertiary = Color(0xFFE0B7C4),
            background = Color(0xFF111111), surface = Color(0xFF1B1B1B)
        ) else lightColorScheme(
            primary = Color(0xFF5F5B66), secondary = Color(0xFF6C6872), tertiary = Color(0xFF7A5863),
            background = Color(0xFFF7F5F8), surface = Color(0xFFFFFFFF)
        )
        else -> if (dark) darkColorScheme(
            primary = Color(0xFFD0BCFF), secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8),
            background = Color(0xFF111014), surface = Color(0xFF1B191F)
        ) else lightColorScheme(
            primary = Color(0xFF6750A4), secondary = Color(0xFF625B71), tertiary = Color(0xFF7D5260),
            background = Color(0xFFFCF8FF), surface = Color(0xFFFFFFFF)
        )
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
