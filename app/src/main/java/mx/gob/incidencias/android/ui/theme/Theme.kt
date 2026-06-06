package mx.gob.incidencias.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

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

private val LightColors: Colors = lightColors(
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

private val DarkColors: Colors = darkColors(
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

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.xs),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.md),
    large = androidx.compose.foundation.shape.RoundedCornerShape(Spacing.lg)
)

private val AppTypography = Typography(defaultFontFamily = FontFamily.SansSerif)

@Composable
fun IncidenciasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
