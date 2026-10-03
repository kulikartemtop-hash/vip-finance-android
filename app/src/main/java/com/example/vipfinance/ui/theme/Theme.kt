package com.example.vipfinance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MidnightLight = lightColorScheme(
    primary = Color(0xFF5B35F5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DEFF),
    onPrimaryContainer = Color(0xFF22005A),
    secondary = Color(0xFF00A7B5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5F5F8),
    onSecondaryContainer = Color(0xFF00363A),
    tertiary = Color(0xFFE24D8B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E7),
    onTertiaryContainer = Color(0xFF3A001B),
    background = Color(0xFFF7F5FF),
    onBackground = Color(0xFF1B1730),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1B1730),
    surfaceVariant = Color(0xFFE9E3F3),
    onSurfaceVariant = Color(0xFF494455),
    outline = Color(0xFF7A7285)
)

private val MidnightDark = darkColorScheme(
    primary = Color(0xFFB9A4FF),
    onPrimary = Color(0xFF2F0B6E),
    primaryContainer = Color(0xFF47228F),
    onPrimaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFF5DE1EA),
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF00545A),
    onSecondaryContainer = Color(0xFF8AF1F6),
    tertiary = Color(0xFFFF8DBA),
    onTertiary = Color(0xFF56002D),
    tertiaryContainer = Color(0xFF7A1648),
    onTertiaryContainer = Color(0xFFFFD9E7),
    background = Color(0xFF0B0914),
    onBackground = Color(0xFFEDE8F5),
    surface = Color(0xFF13101D),
    onSurface = Color(0xFFEDE8F5),
    surfaceVariant = Color(0xFF282331),
    onSurfaceVariant = Color(0xFFCCC4D4),
    outline = Color(0xFF958C9F)
)

private val PlatinumLight = lightColorScheme(
    primary = Color(0xFF345A7A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E9FF),
    onPrimaryContainer = Color(0xFF0A1D2D),
    secondary = Color(0xFF5D6670),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E6EC),
    onSecondaryContainer = Color(0xFF182028),
    tertiary = Color(0xFF6D5B91),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF25133E),
    background = Color(0xFFF3F6F9),
    onBackground = Color(0xFF171C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C20),
    surfaceVariant = Color(0xFFE4E9EE),
    onSurfaceVariant = Color(0xFF454C52),
    outline = Color(0xFF737B83)
)

private val PlatinumDark = darkColorScheme(
    primary = Color(0xFFA9CBEA),
    onPrimary = Color(0xFF10344D),
    primaryContainer = Color(0xFF214A68),
    onPrimaryContainer = Color(0xFFD5E9FF),
    secondary = Color(0xFFC2CBD4),
    onSecondary = Color(0xFF2B333A),
    secondaryContainer = Color(0xFF414B54),
    onSecondaryContainer = Color(0xFFE0E6EC),
    tertiary = Color(0xFFD8C3FF),
    onTertiary = Color(0xFF38234F),
    tertiaryContainer = Color(0xFF4E3967),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF0F1215),
    onBackground = Color(0xFFE6E9EC),
    surface = Color(0xFF171B1F),
    onSurface = Color(0xFFE6E9EC),
    surfaceVariant = Color(0xFF292F34),
    onSurfaceVariant = Color(0xFFC2C9CF),
    outline = Color(0xFF8C949B)
)

private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF007A5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8FF5D1),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF3F6B58),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2E9D6),
    onSecondaryContainer = Color(0xFF0D1F16),
    tertiary = Color(0xFF7B5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF251A00),
    background = Color(0xFFF1FAF5),
    onBackground = Color(0xFF121C17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121C17),
    surfaceVariant = Color(0xFFDDEBE4),
    onSurfaceVariant = Color(0xFF3F4A44),
    outline = Color(0xFF6D7872)
)

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF5BFFBE),
    onPrimary = Color(0xFF003827),
    primaryContainer = Color(0xFF00533E),
    onPrimaryContainer = Color(0xFF7DFFCB),
    secondary = Color(0xFFA7D8BF),
    onSecondary = Color(0xFF153529),
    secondaryContainer = Color(0xFF2B4E3E),
    onSecondaryContainer = Color(0xFFC2E9D6),
    tertiary = Color(0xFFFFD95B),
    onTertiary = Color(0xFF3D2D00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF07130E),
    onBackground = Color(0xFFE0F1E8),
    surface = Color(0xFF0D1D15),
    onSurface = Color(0xFFE0F1E8),
    surfaceVariant = Color(0xFF1D3428),
    onSurfaceVariant = Color(0xFFB8CCC1),
    outline = Color(0xFF748B7F)
)

private val VipShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val VipTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

@Composable
fun VIPFinanceTheme(
    theme: String = "system",
    style: String = "platinum",
    content: @Composable () -> Unit
) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colors = when (style) {
        "midnight", "classic" -> if (dark) MidnightDark else MidnightLight
        "emerald", "ocean" -> if (dark) EmeraldDark else EmeraldLight
        "platinum", "graphite" -> if (dark) PlatinumDark else PlatinumLight
        else -> if (dark) MidnightDark else MidnightLight
    }

    MaterialTheme(
        colorScheme = colors,
        typography = VipTypography,
        shapes = VipShapes,
        content = content
    )
}
