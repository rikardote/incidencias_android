package mx.gob.incidencias.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.Shapes as Material2Shapes
import androidx.compose.material.Typography as Material2Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.material.MaterialTheme as Material2Theme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Shapes as Material3Shapes
import androidx.compose.material3.Typography as Material3Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// Institutional colors
val Guinda = Color(0xFF9B2247)
val GuindaDark = Color(0xFF611232)
val GuindaLight = Color(0xFFD4A5B8)
val Oro = Color(0xFFA57F2C)
val OroLight = Color(0xFFE6D194)
val OroDark = Color(0xFF6B5118)
val Verde = Color(0xFF1E5B4F)
val VerdeDark = Color(0xFF13322B)
val VerdeLight = Color(0xFF7FB8AD)

// Semantic colors
val Success = Color(0xFF2E7D32)
val SuccessContainer = Color(0xFFC8E6C9)
val Warning = Color(0xFFF57C00)
val WarningContainer = Color(0xFFFFE0B2)
val Info = Color(0xFF0277BD)
val InfoContainer = Color(0xFFB3E5FC)

// Spacing scale (4dp grid)
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Guinda,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4D6E0),
    onPrimaryContainer = GuindaDark,
    secondary = Oro,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E4BE),
    onSecondaryContainer = OroDark,
    tertiary = Verde,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3E8E3),
    onTertiaryContainer = VerdeDark,
    error = Color(0xFFB91C1C),
    onError = Color.White,
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF1F2937),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFE8EAEE),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFD1D5DB)
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = GuindaLight,
    onPrimary = Color(0xFF32101D),
    primaryContainer = Guinda,
    onPrimaryContainer = Color.White,
    secondary = OroLight,
    onSecondary = Color(0xFF2F250B),
    secondaryContainer = OroDark,
    onSecondaryContainer = Color(0xFFFFF1C9),
    tertiary = VerdeLight,
    onTertiary = Color(0xFF08241E),
    tertiaryContainer = Verde,
    onTertiaryContainer = Color.White,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF3A0808),
    background = Color(0xFF111827),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF1F2937),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFF4B5563)
)

// Compatibility layer while screens are migrated progressively from Material2 to Material3.
private val LightMaterial2Colors: Colors = lightColors(
    primary = Guinda,
    primaryVariant = GuindaDark,
    secondary = Oro,
    secondaryVariant = OroDark,
    background = Color(0xFFF4F6F9),
    surface = Color.White,
    error = Color(0xFFB91C1C),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F2937),
    onSurface = Color(0xFF1F2937),
    onError = Color.White
)

private val DarkMaterial2Colors: Colors = darkColors(
    primary = GuindaLight,
    primaryVariant = Guinda,
    secondary = OroLight,
    secondaryVariant = Oro,
    background = Color(0xFF111827),
    surface = Color(0xFF1F2937),
    error = Color(0xFFFCA5A5),
    onPrimary = Color(0xFF1F2937),
    onSecondary = Color(0xFF1F2937),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
    onError = Color(0xFF1F2937)
)

private val AppMaterial3Shapes = Material3Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.xs),
    small = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.sm),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.md),
    large = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.lg),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

private val AppMaterial2Shapes = Material2Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.xs),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.md),
    large = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.lg)
)

private val AppMaterial3Typography = Material3Typography()
private val AppMaterial2Typography = Material2Typography(defaultFontFamily = FontFamily.SansSerif)

@Composable
fun IncidenciasTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    Material3Theme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppMaterial3Typography,
        shapes = AppMaterial3Shapes
    ) {
        Material2Theme(
            colors = if (darkTheme) DarkMaterial2Colors else LightMaterial2Colors,
            typography = AppMaterial2Typography,
            shapes = AppMaterial2Shapes,
            content = content
        )
    }
}
