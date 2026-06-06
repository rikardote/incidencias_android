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

val Guinda = Color(0xFF9B2247)
val GuindaDark = Color(0xFF611232)
val Oro = Color(0xFFA57F2C)
val OroLight = Color(0xFFE6D194)
val Verde = Color(0xFF1E5B4F)
val VerdeDark = Color(0xFF13322B)

private val LightColors: Colors = lightColors(
    primary = Guinda,
    primaryVariant = GuindaDark,
    secondary = Oro,
    background = Color(0xFFF4F6F9),
    surface = Color.White,
    error = Color(0xFFB91C1C),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1F2937),
    onSurface = Color(0xFF1F2937)
)

private val DarkColors: Colors = darkColors(
    primary = OroLight,
    primaryVariant = Oro,
    secondary = OroLight,
    background = Color(0xFF111827),
    surface = Color(0xFF1F2937),
    error = Color(0xFFFCA5A5),
    onPrimary = VerdeDark,
    onSecondary = VerdeDark,
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6)
)

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
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
