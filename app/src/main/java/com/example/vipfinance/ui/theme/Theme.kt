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
    primary = Color(0xFF111827), onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E7EB), onPrimaryContainer = Color(0xFF111827),
    secondary = Color(0xFF64748B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0), onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFFB08D57), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8D0), onTertiaryContainer = Color(0xFF3D2B12),
    background = Color(0xFFF5F5F2), onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE8E8E4), onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFF8B8B86)
)

private val PlatinumDark = darkColorScheme(
    primary = Color(0xFFF4F4F0), onPrimary = Color(0xFF171717),
    primaryContainer = Color(0xFF2B2B29), onPrimaryContainer = Color(0xFFF4F4F0),
    secondary = Color(0xFFB5B5B0), onSecondary = Color(0xFF202020),
    secondaryContainer = Color(0xFF343432), onSecondaryContainer = Color(0xFFE4E4DF),
    tertiary = Color(0xFFD4AF6A), onTertiary = Color(0xFF2D210D),
    tertiaryContainer = Color(0xFF5C4721), onTertiaryContainer = Color(0xFFF3E8D0),
    background = Color(0xFF050505), onBackground = Color(0xFFF0F0EC),
    surface = Color(0xFF0D0D0D), onSurface = Color(0xFFF0F0EC),
    surfaceVariant = Color(0xFF191918), onSurfaceVariant = Color(0xFFB9B9B4),
    outline = Color(0xFF666661)
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


private val RoyalLight = lightColorScheme(
    primary = Color(0xFF6A3DE8), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF), onPrimaryContainer = Color(0xFF24005A),
    secondary = Color(0xFFB88700), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7A8), onSecondaryContainer = Color(0xFF241A00),
    tertiary = Color(0xFFB52D68), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E7), onTertiaryContainer = Color(0xFF3B001A),
    background = Color(0xFFFAF7FF), onBackground = Color(0xFF1D1728),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF1D1728),
    surfaceVariant = Color(0xFFEDE5F4), onSurfaceVariant = Color(0xFF4D4554),
    outline = Color(0xFF7E7487)
)
private val RoyalDark = darkColorScheme(
    primary = Color(0xFFD1B8FF), onPrimary = Color(0xFF3B0878),
    primaryContainer = Color(0xFF5522A1), onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFFFD66B), onSecondary = Color(0xFF3B2D00),
    secondaryContainer = Color(0xFF5A4600), onSecondaryContainer = Color(0xFFFFE7A8),
    tertiary = Color(0xFFFFA9C8), onTertiary = Color(0xFF5A0030),
    tertiaryContainer = Color(0xFF7D1649), onTertiaryContainer = Color(0xFFFFD9E7),
    background = Color(0xFF0D0915), onBackground = Color(0xFFF1EAF8),
    surface = Color(0xFF17111F), onSurface = Color(0xFFF1EAF8),
    surfaceVariant = Color(0xFF2A2133), onSurfaceVariant = Color(0xFFD0C5D7),
    outline = Color(0xFF978A9F)
)
private val RoseLight = lightColorScheme(
    primary = Color(0xFFB52D68), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E7), onPrimaryContainer = Color(0xFF3B001A),
    secondary = Color(0xFF6B5B8C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE1FF), onSecondaryContainer = Color(0xFF25133E),
    tertiary = Color(0xFF006E6A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9CF1EB), onTertiaryContainer = Color(0xFF00201F),
    background = Color(0xFFFFF8FA), onBackground = Color(0xFF21171B),
    surface = Color(0xFFFFFBFC), onSurface = Color(0xFF21171B),
    surfaceVariant = Color(0xFFF1E3E7), onSurfaceVariant = Color(0xFF514348),
    outline = Color(0xFF817177)
)
private val RoseDark = darkColorScheme(
    primary = Color(0xFFFFB0CC), onPrimary = Color(0xFF65002F),
    primaryContainer = Color(0xFF8C174E), onPrimaryContainer = Color(0xFFFFD9E7),
    secondary = Color(0xFFD8C4FF), onSecondary = Color(0xFF38234F),
    secondaryContainer = Color(0xFF503968), onSecondaryContainer = Color(0xFFEDE1FF),
    tertiary = Color(0xFF70DED8), onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF00504D), onTertiaryContainer = Color(0xFF9CF1EB),
    background = Color(0xFF130B0F), onBackground = Color(0xFFF3E9EC),
    surface = Color(0xFF1D1217), onSurface = Color(0xFFF3E9EC),
    surfaceVariant = Color(0xFF342329), onSurfaceVariant = Color(0xFFD5C4C9),
    outline = Color(0xFF9C858C)
)

private val VipShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(19.dp),
    extraLarge = RoundedCornerShape(25.dp)
)

private val VipTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 35.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp)
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
        "royal" -> if (dark) RoyalDark else RoyalLight
        "rose" -> if (dark) RoseDark else RoseLight
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
