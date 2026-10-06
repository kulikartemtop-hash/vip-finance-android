
// ============================================================================
package com.example.vipfinance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- MIDNIGHT THEME (Graphite / Deep Violet / Platinum) ---
private val MidnightLight = lightColorScheme(
    primary = Color(0xFF5B35F5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DEFF),
    onPrimaryContainer = Color(0xFF22005A),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFEF4444)
)

private val MidnightDark = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFF0F0518),
    primaryContainer = Color(0xFF2E1065),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF09040F),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF120A1F),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E1B2E),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFEF4444)
)

// --- PLATINUM THEME (Cool Graphite / Silver / Ivory) ---
private val PlatinumLight = lightColorScheme(
    primary = Color(0xFF334155),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFEF4444)
)

private val PlatinumDark = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color(0xFFF8FAFC),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF151A23),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = Color(0xFFEF4444)
)

// --- EMERALD THEME (Deep Green / Platinum / Dark Graphite) ---
private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    background = Color(0xFFECFDF5),
    onBackground = Color(0xFF064E3B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFCCFBF1),
    onSurfaceVariant = Color(0xFF115E59),
    outline = Color(0xFF5EEAD4),
    error = Color(0xFFEF4444)
)

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF115E59),
    background = Color(0xFF020604),
    onBackground = Color(0xFFD1FAE5),
    surface = Color(0xFF0F1713),
    onSurface = Color(0xFFD1FAE5),
    surfaceVariant = Color(0xFF132E25),
    onSurfaceVariant = Color(0xFF5EEAD4),
    outline = Color(0xFF065F46),
    error = Color(0xFFEF4444)
)
// --- ROYAL THEME (Deep Violet / Champagne Gold / Graphite) ---
private val RoyalLight = lightColorScheme(
    primary = Color(0xFFB4941F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4E4BC),
    onPrimaryContainer = Color(0xFF3D2E00),
    secondary = Color(0xFF6D28D9),
    onSecondary = Color.White,
    background = Color(0xFFFAF7FF),
    onBackground = Color(0xFF1E1B4B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFEDE9FE),
    onSurfaceVariant = Color(0xFF4C1D95),
    outline = Color(0xFFA78BFA),
    error = Color(0xFFEF4444)
)

private val RoyalDark = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF1A0B2E),
    primaryContainer = Color(0xFF2D1B4E),
    onPrimaryContainer = Color(0xFFF4E4BC),
    secondary = Color(0xFF9D84B7),
    onSecondary = Color(0xFF1A0B2E),
    background = Color(0xFF0D0915),
    onBackground = Color(0xFFF4E4BC),
    surface = Color(0xFF151020),
    onSurface = Color(0xFFF4E4BC),
    surfaceVariant = Color(0xFF231838),
    onSurfaceVariant = Color(0xFFE9D5FF),
    outline = Color(0xFF4C1D95),
    error = Color(0xFFEF4444)
)

// --- ROSE THEME (Refined Rose / Teal / Graphite) ---
private val RoseLight = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF99F6E4),
    onPrimaryContainer = Color(0xFF115E59),
    secondary = Color(0xFFE11D48),
    onSecondary = Color.White,
    background = Color(0xFFFFF1F2),
    onBackground = Color(0xFF881337),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF881337),
    surfaceVariant = Color(0xFFFFE4E6),
    onSurfaceVariant = Color(0xFF9F1239),
    outline = Color(0xFFFDA4AF),
    error = Color(0xFFEF4444)
)

private val RoseDark = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = Color(0xFFFB7185),
    onSecondary = Color(0xFF881337),
    background = Color(0xFF0C0508),
    onBackground = Color(0xFFFDF2F8),
    surface = Color(0xFF180B10),
    onSurface = Color(0xFFFDF2F8),
    surfaceVariant = Color(0xFF2D111A),
    onSurfaceVariant = Color(0xFFFDA4AF),
    outline = Color(0xFF881337),
    error = Color(0xFFEF4444)
)

// --- SHAPES & TYPOGRAPHY ---
private val VipShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val VipTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )
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